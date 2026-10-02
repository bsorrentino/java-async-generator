package org.bsc.async.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
         * Calls {@code listener} once with {@code mayInterruptIfRunning}, on the thread that cancels the generator,
         * or immediately if it is already cancelled.
         *
         * @return removes the listener when closed
         */
        Registration onCancel( Consumer<Boolean> listener );

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
         * Like {@link #build(Consumer)}, also handing the emitter the generator's {@link CancellationToken}.
         * Data dispatched after cancellation is discarded.
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

        private static final class ListenerEntry {
            private final Consumer<Boolean> listener;

            ListenerEntry( Consumer<Boolean> listener ) {
                this.listener = listener;
            }

            Consumer<Boolean> listener() {
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
            public Registration onCancel(Consumer<Boolean> listener) {
                return Generator.this.onCancel(listener);
            }
        };

        private final Object lock = new Object();
        // guarded by lock
        private Thread consumerThread;
        private boolean consumerInterruptedByCancel;
        private Thread emitterThread;
        private boolean emitterInDispatchSync;
        private boolean emitterInterruptedByCancel;
        private Boolean cancelledWithInterrupt;
        private List<ListenerEntry> cancelListeners = new ArrayList<>();

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
                synchronized (lock) {
                    consumerThread = null;
                    if( consumerInterruptedByCancel ) {
                        consumerInterruptedByCancel = false;
                        Thread.interrupted();
                    }
                }
            }
            if( interruptedByOther ) {
                cancel( false );
                Thread.currentThread().interrupt();
            }
            return end( Data.done(CANCELLED) );
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( !super.cancel(mayInterruptIfRunning) ) {
                return false;
            }
            endData.compareAndSet( null, Data.done(CANCELLED) );

            final List<ListenerEntry> listeners;
            synchronized (lock) {
                cancelledWithInterrupt = mayInterruptIfRunning;
                listeners = cancelListeners;
                cancelListeners = null;
            }
            // listeners first: a linked child whose consumer is our emitter must be cancelled with our flag
            // before our interrupt wakes that consumer, which would otherwise cancel the child without interrupt
            for( var entry : listeners ) {
                notifyListener( entry.listener(), mayInterruptIfRunning );
            }
            synchronized (lock) {
                if( consumerThread != null ) {
                    consumerInterruptedByCancel = interruptUnlessAlreadyInterrupted( consumerThread );
                }
                if( emitterThread != null && (mayInterruptIfRunning || emitterInDispatchSync) ) {
                    emitterInterruptedByCancel = interruptUnlessAlreadyInterrupted( emitterThread );
                }
            }
            releaseParentLink();
            return true;
        }

        private static void notifyListener( Consumer<Boolean> listener, boolean mayInterruptIfRunning ) {
            try {
                listener.accept( mayInterruptIfRunning );
            }
            catch( Throwable ex ) {
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

        private CancellationToken.Registration onCancel( Consumer<Boolean> listener ) {
            requireNonNull(listener, "listener cannot be null");
            final boolean mayInterruptIfRunning;
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
                mayInterruptIfRunning = cancelledWithInterrupt;
            }
            notifyListener( listener, mayInterruptIfRunning );
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
                            if( Boolean.TRUE.equals(cancelledWithInterrupt) || !emitterInterruptedByCancel ) {
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
                            if( emitterInterruptedByCancel && !Boolean.TRUE.equals(cancelledWithInterrupt) ) {
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
            try {
                if( !cancelledBeforeStart ) {
                    emitter.run();
                }
            }
            finally {
                synchronized (lock) {
                    emitterThread = null;
                    // an interrupt from elsewhere must survive
                    if( emitterInterruptedByCancel ) {
                        emitterInterruptedByCancel = false;
                        Thread.interrupted();
                    }
                }
            }
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData.get() ).map( Data::resultValue );
        }
    }


}
