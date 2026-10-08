package org.bsc.async.v5;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.bsc.async.AsyncGenerator.*;

/**
 * Represents a queue-based asynchronous generator.
 */
public interface AsyncGeneratorFlow {

    interface IsCancellableEx extends IsCancellable{

        @FunctionalInterface
        interface Listener {
            void cancelled(boolean mayInterruptIfRunning);
        }

        interface Registration extends AutoCloseable {
            static Registration noop() {
                return () -> {};
            }

            @Override
            void close();
        }

        /**
         * Calls the listener right away if already cancelled.
         */
        Registration onCancel(Listener listener);
    }

    interface Dispatcher<E> extends IsCancellableEx {
        void dispatchSync( Data<E> data ) throws InterruptedException;
        void dispatchAsync( Data<E> data );

        @Override
        default Registration onCancel(Listener listener) {
            throw new UnsupportedOperationException("operation not supported outside of Generator");
        }

        @Override
        default boolean isCancelled() {
            return false;
        }

        @Override
        default boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException("operation not supported outside of Generator");
        }
    }

    interface Receiver<E> {
        Data<E> waitSync() throws InterruptedException;
        Optional<Data<E>> waitAsync();

    }

    interface Processor<E>  {
        Dispatcher<E> dispatcher();
        Receiver<E> receiver();
    }

    class Builder {
        private Processor<?> processor;
        private Executor executor;
        private IsCancellableEx parent;

        public <E> Builder processor( Processor<E> processor) {
            this.processor = processor;
            return this;
        }
        public Builder executor( Executor executor) {
            this.executor = executor;
            return this;
        }
        public Builder cancelledBy( IsCancellableEx parent ) {
            this.parent = requireNonNull(parent, "parent cannot be null");
            return this;
        }

        /**
         * After a cancel, {@code dispatchAsync} drops silently and {@code dispatchSync} throws {@link InterruptedException}.
         */
        public <E> Generator<E> build( Consumer<Dispatcher<E>> emitter ) {

            final var result = this.<E>build();

            try {
                CompletableFuture.runAsync(
                        () -> emitter.accept(result.cancellableDispatcher),
                        result.cancellableDispatcher.executor() );
            }
            catch( RuntimeException e ) {
                result.unlinkFromParent();
                throw e;
            }

            return result;
        }

        @SuppressWarnings("unchecked")
        public <E> Generator<E> build() {
            if( processor == null ) {
                processor = new BlockingQueueProcessor<>();
            }
            final var dispatcher = (Dispatcher<E>)processor.dispatcher();
            final var cancellableDispatcher = new CancellableDispatcher<>(dispatcher, executor);

            final var result = new AsyncGeneratorFlow.Generator<>( (Receiver<E>)processor.receiver(), cancellableDispatcher );

            if( parent != null ) {
                result.linkToParent(parent);
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
    class Generator<E> extends BaseCancellable<E> implements HasResultValue, IsCancellableEx {

        static final Executor DEFAULT_EXECUTOR = CompletableFuture::runAsync;

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final Receiver<E> receiver;
        final CancellableDispatcher<E> cancellableDispatcher;
        private final AtomicReference<Registration> parentRegistration = new AtomicReference<>();

        Generator(Receiver<E> receiver, CancellableDispatcher<E> cancellableDispatcher) {
            this.receiver = requireNonNull(receiver, "receiver cannot be null");
            this.cancellableDispatcher = requireNonNull(cancellableDispatcher, "cancellableDispatcher cannot be null");
        }

        public Receiver<E> receiver() {
            return receiver;
        }

        private boolean isEnded() {
            return endData != null;
        }

        /**
         * Retrieves the next element from the queue asynchronously.
         *
         * @return the next element from the queue
         */
        @Override
        public Data<E> next() {
            if( !isEnded() && isCancelled() ) {
                return endCancelled();
            }
            if( isEnded() ) {
                return endData;
            }
            if(executorThread!=null) {
                return end(Data.error(new IllegalStateException("illegal concurrent next() invocation")));
            }
            executorThread = Thread.currentThread();
            try {
                Data<E> value = null;
                while( true ) {
                    value = cancellableDispatcher.executor().callUntilCancelled(receiver::waitSync);

                    if (value.isDone() ) {
                        return complete(value);
                    }
                    break;
                }
                return value;
            } catch (InterruptedException e) {
                if( !isCancelled() ) {
                    cancel(false);
                    Thread.currentThread().interrupt();
                }
                return endCancelled();
            }
            finally {
                executorThread = null;
            }
        }

        private Data<E> complete(Data<E> data) {
            cancellableDispatcher.complete();
            return end(data);
        }

        private Data<E> endCancelled() {
            return end(Data.done(CANCELLED));
        }

        private Data<E> end(Data<E> data) {
            endData = data;
            unlinkFromParent();
            return data;
        }

        /**
         * Once the last element has been returned, a cancel keeps the result and does not reach the generators linked
         * with {@link Builder#cancelledBy}.
         */
        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( !super.cancel(mayInterruptIfRunning) ) {
                return false;
            }
            unlinkFromParent();

            return cancellableDispatcher.cancel(mayInterruptIfRunning);

        }

        @Override
        public Registration onCancel(Listener listener) {
            return cancellableDispatcher.onCancel(listener);

        }

        void linkToParent(IsCancellableEx parent) {
            parentRegistration.set(parent.onCancel(this::cancel));
        }

        void unlinkFromParent() {
            final var registration = parentRegistration.getAndSet(null);
            if( registration != null ) {
                registration.close();
            }
        }

        @Override
        public void close() {
            unlinkFromParent();
            super.close();
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData ).map( Data::resultValue );
        }
    }

}
