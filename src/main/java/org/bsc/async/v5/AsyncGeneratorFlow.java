package org.bsc.async.v5;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.bsc.async.AsyncGenerator.*;

/**
 * Represents a queue-based asynchronous generator.
 */
public interface AsyncGeneratorFlow {

    interface Dispatcher<E> {
        void dispatchSync( Data<E> data ) throws InterruptedException;
        void dispatchAsync( Data<E> data );
    }

    interface Receiver<E> {
        Data<E> waitSync() throws InterruptedException;
        Optional<Data<E>> waitAsync();

    }

    interface Processor<E> extends Dispatcher<E>, Receiver<E> {
    }

    interface CancellationToken {

        boolean isCancelled();

        /**
         * Calls {@code listener} once, on the thread that cancels the generator, before the generator interrupts
         * its own threads, or immediately if it is already cancelled.
         *
         * @return removes the listener when closed
         */
        Registration onCancel( Listener listener );

        @FunctionalInterface
        interface Listener {
            void cancelled( boolean mayInterruptIfRunning );
        }

        interface Registration extends AutoCloseable {
            @Override
            void close();
        }
    }

    class Builder {
        private Processor<?> processor;
        private Executor executor;
        private CancellationToken parent;

        public <E> Builder processor( Processor<E> processor) {
            this.processor = processor;
            return this;
        }
        public Builder executor( Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Cancels the generator, with the same {@code mayInterruptIfRunning}, when {@code parent} is cancelled.
         * The link lasts until the generator's stream ends, either one is cancelled, or the generator is closed.
         */
        public Builder cancelledBy( CancellationToken parent ) {
            this.parent = requireNonNull(parent, "parent cannot be null");
            return this;
        }

        public <E> Generator<E> build( Consumer<Dispatcher<E>> emitter ) {
            requireNonNull(emitter, "emitter cannot be null");
            return this.<E>build( (dispatcher, cancellation) -> emitter.accept(dispatcher) );
        }

        /**
         * Data the emitter dispatches after cancellation is discarded.
         */
        @SuppressWarnings("unchecked")
        public <E> Generator<E> build( BiConsumer<Dispatcher<E>, CancellationToken> emitter ) {
            requireNonNull(emitter, "emitter cannot be null");
            final Generator<E> result = this.build();

            final var dispatcher = result.emitterDispatcher( (Dispatcher<E>) processor );
            final Runnable emitterTask = () ->
                    result.runEmitter( () -> emitter.accept( dispatcher, result.cancellationToken() ) );

            try {
                if( executor != null ) {
                    CompletableFuture.runAsync( emitterTask, executor );
                }
                else {
                    CompletableFuture.runAsync( emitterTask );
                }
            }
            catch( RejectedExecutionException ex ) {
                result.releaseParentLink();
                throw ex;
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        public <E> Generator<E> build() {
            if( processor == null ) {
                processor = new BlockingQueueProcessor<>();
            }
            final var result = new AsyncGeneratorFlow.Generator<>( (Receiver<E>)processor );
            if( parent != null ) {
                result.linkTo( parent );
            }
            return result;
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static <E> Generator<E> create( Consumer<Dispatcher<E>> emitter ) {
        return builder().build( emitter );
    }

    static <E> Generator<E> create( Processor<E> processor ) {
        return builder().processor(processor).build();
    }

    /**
     * Inner class to generate asynchronous elements from the queue.
     *
     * @param <E> the type of elements in the queue
     */
    class Generator<E> extends BaseCancellable<E> implements HasResultValue {

        private static final System.Logger log = System.getLogger(AsyncGeneratorFlow.class.getName());
        private static final ThreadLocal<Generator<?>> runningEmitter = new ThreadLocal<>();

        private static final class ListenerEntry {
            private final CancellationToken.Listener listener;

            ListenerEntry( CancellationToken.Listener listener ) {
                this.listener = listener;
            }

            CancellationToken.Listener listener() {
                return listener;
            }
        }

        private final Receiver<E> receiver;
        private final AtomicReference<Data<E>> endData = new AtomicReference<>();
        private final AtomicReference<CancellationToken.Registration> parentLink = new AtomicReference<>();
        private final CancellationToken cancellation = new CancellationToken() {
            @Override
            public boolean isCancelled() {
                return Generator.this.isCancelled();
            }

            @Override
            public Registration onCancel(Listener listener) {
                return Generator.this.onCancel(listener);
            }
        };
        private volatile boolean cancelledWithInterrupt;
        private volatile boolean completed;

        private final Object lock = new Object();
        // guarded by lock
        private Thread consumerThread;
        private boolean consumerInterruptedByCancel;
        private Thread emitterThread;
        private boolean emitterInDispatchSync;
        private boolean emitterInterruptedByCancel;
        private Set<ListenerEntry> cancelListeners = new LinkedHashSet<>();
        private boolean listenersDroppedOnCompletion;

        public Generator(Receiver<E> receiver ) {
            this.receiver = requireNonNull(receiver, "receiver cannot be null");
        }

        public Receiver<E> receiver() {
            return receiver;
        }

        public CancellationToken cancellationToken() {
            return cancellation;
        }

        private Data<E> end( Data<E> data ) {
            if( data.resultValue() != CANCELLED ) {
                completed = true;
            }
            endData.compareAndSet( null, data );
            releaseParentLink();
            return endData.get();
        }

        @Override
        public Data<E> next() {
            final var ended = endData.get();
            if( ended != null ) {
                return ended;
            }
            final boolean concurrentNext;
            synchronized (lock) {
                concurrentNext = consumerThread != null;
                if( !concurrentNext ) {
                    consumerThread = Thread.currentThread();
                }
            }
            if( concurrentNext ) {
                endData.compareAndSet( null, Data.error(new IllegalStateException("illegal concurrent next() invocation")) );
                return endData.get();
            }

            boolean interruptedByOther = false;
            try {
                if( isCancelled() ) {
                    return end( Data.done(CANCELLED) );
                }
                final Data<E> value = receiver.waitSync();
                if( value.isDone() ) {
                    return end( value );
                }
                if( isCancelled() ) {
                    return end( Data.done(CANCELLED) );
                }
                return value;
            } catch (InterruptedException e) {
                synchronized (lock) {
                    interruptedByOther = !consumerInterruptedByCancel || Thread.currentThread().isInterrupted();
                }
            }
            finally {
                final boolean ownInterrupt;
                synchronized (lock) {
                    consumerThread = null;
                    ownInterrupt = consumerInterruptedByCancel;
                    consumerInterruptedByCancel = false;
                }
                if( ownInterrupt ) {
                    releaseOwnInterrupt();
                }
            }
            if( interruptedByOther ) {
                cancel( false );
                Thread.currentThread().interrupt();
            }
            return end( Data.done(CANCELLED) );
        }

        /**
         * Clears an interrupt this generator sent to the current thread, unless an enclosing emitter running on the
         * same thread was cancelled with interrupt too: that emitter's own interrupt found the thread already
         * interrupted by us, so the interrupt is handed over to it instead.
         */
        private static void releaseOwnInterrupt() {
            final var enclosing = runningEmitter.get();
            if( enclosing != null && enclosing.isCancelled() && enclosing.cancelledWithInterrupt ) {
                Thread.currentThread().interrupt();
                enclosing.adoptEmitterInterrupt();
            }
            else {
                Thread.interrupted();
            }
        }

        private void adoptEmitterInterrupt() {
            synchronized (lock) {
                if( emitterThread == Thread.currentThread() ) {
                    emitterInterruptedByCancel = true;
                }
            }
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( !super.cancel(mayInterruptIfRunning) ) {
                return false;
            }
            if( !endData.compareAndSet( null, Data.done(CANCELLED) ) && completed ) {
                // a stream that already ended has nothing left to stop: linked generators started from it live on
                synchronized (lock) {
                    cancelListeners = null;
                    listenersDroppedOnCompletion = true;
                }
                releaseParentLink();
                return true;
            }
            cancelledWithInterrupt = mayInterruptIfRunning;

            final Set<ListenerEntry> listeners;
            synchronized (lock) {
                listeners = cancelListeners;
                cancelListeners = null;
            }
            // before interrupting: our emitter may be a linked child's consumer, and waking it first cancels the child without interrupt
            for( var entry : listeners ) {
                notifyListener( entry.listener(), mayInterruptIfRunning );
            }
            synchronized (lock) {
                if( consumerThread != null ) {
                    consumerInterruptedByCancel |= interruptUnlessAlreadyInterrupted( consumerThread );
                }
                if( emitterThread != null && (mayInterruptIfRunning || emitterInDispatchSync) ) {
                    emitterInterruptedByCancel |= interruptUnlessAlreadyInterrupted( emitterThread );
                }
            }
            releaseParentLink();
            return true;
        }

        @Override
        public void close() {
            super.close();
            releaseParentLink();
        }

        private static void notifyListener( CancellationToken.Listener listener, boolean mayInterruptIfRunning ) {
            try {
                listener.cancelled( mayInterruptIfRunning );
            }
            catch( RuntimeException ex ) {
                log.log( System.Logger.Level.WARNING, "cancellation listener failed", ex );
            }
        }

        private static boolean interruptUnlessAlreadyInterrupted( Thread thread ) {
            if( thread.isInterrupted() ) {
                return false;
            }
            thread.interrupt();
            return true;
        }

        private CancellationToken.Registration onCancel( CancellationToken.Listener listener ) {
            requireNonNull(listener, "listener cannot be null");
            synchronized (lock) {
                if( cancelListeners != null ) {
                    final var entry = new ListenerEntry( listener );
                    cancelListeners.add( entry );
                    return () -> {
                        synchronized (lock) {
                            if( cancelListeners != null ) {
                                cancelListeners.remove( entry );
                            }
                        }
                    };
                }
                if( listenersDroppedOnCompletion ) {
                    return () -> {};
                }
            }
            notifyListener( listener, cancelledWithInterrupt );
            return () -> {};
        }

        private void linkTo( CancellationToken parent ) {
            parentLink.set( parent.onCancel( this::cancel ) );
        }

        private void releaseParentLink() {
            final var link = parentLink.getAndSet( null );
            if( link != null ) {
                link.close();
            }
        }

        private Dispatcher<E> emitterDispatcher( Dispatcher<E> target ) {
            return new Dispatcher<>() {
                @Override
                public void dispatchSync(Data<E> data) throws InterruptedException {
                    synchronized (lock) {
                        if( isCancelled() ) {
                            return;
                        }
                        emitterInDispatchSync = true;
                    }
                    try {
                        target.dispatchSync( data );
                    }
                    catch( InterruptedException ex ) {
                        synchronized (lock) {
                            if( !isCancelled() ) {
                                throw ex;
                            }
                            if( cancelledWithInterrupt || !emitterInterruptedByCancel ) {
                                Thread.currentThread().interrupt();
                            }
                            else {
                                emitterInterruptedByCancel = false;
                            }
                        }
                    }
                    finally {
                        synchronized (lock) {
                            emitterInDispatchSync = false;
                            if( emitterInterruptedByCancel && !cancelledWithInterrupt ) {
                                emitterInterruptedByCancel = false;
                                Thread.interrupted();
                            }
                        }
                    }
                }

                @Override
                public void dispatchAsync(Data<E> data) {
                    if( !isCancelled() ) {
                        target.dispatchAsync( data );
                    }
                }
            };
        }

        private void runEmitter( Runnable emitter ) {
            final boolean cancelledBeforeStart;
            synchronized (lock) {
                cancelledBeforeStart = isCancelled();
                if( !cancelledBeforeStart ) {
                    emitterThread = Thread.currentThread();
                }
            }
            final var enclosing = runningEmitter.get();
            runningEmitter.set( this );
            try {
                if( !cancelledBeforeStart ) {
                    emitter.run();
                }
            }
            finally {
                if( enclosing != null ) {
                    runningEmitter.set( enclosing );
                }
                else {
                    runningEmitter.remove();
                }
                final boolean ownInterrupt;
                synchronized (lock) {
                    emitterThread = null;
                    ownInterrupt = emitterInterruptedByCancel;
                    emitterInterruptedByCancel = false;
                }
                if( ownInterrupt ) {
                    releaseOwnInterrupt();
                }
            }
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData.get() ).map( Data::resultValue );
        }
    }


}
