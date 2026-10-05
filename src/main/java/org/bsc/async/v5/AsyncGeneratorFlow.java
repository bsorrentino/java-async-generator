package org.bsc.async.v5;

import org.bsc.async.executor.CancellableExecutor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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

        default boolean isCancelled() {
            return false;
        }
    }

    interface Receiver<E> {
        Data<E> waitSync() throws InterruptedException;
        Optional<Data<E>> waitAsync();

    }

    interface Processor<E> extends Dispatcher<E>, Receiver<E> {
    }

    class Builder {
        private Processor<?> processor;
        private Executor executor;

        public <E> Builder processor( Processor<E> processor) {
            this.processor = processor;
            return this;
        }
        public Builder executor( Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * After a cancel, {@code dispatchAsync} drops silently and {@code dispatchSync} throws {@link InterruptedException}.
         */
        @SuppressWarnings("unchecked")
        public <E> Generator<E> build( Consumer<Dispatcher<E>> emitter ) {
            final var result = this.<E>build();
            final var dispatcher = new GeneratorDispatcher<>((Dispatcher<E>) processor, result);

            CompletableFuture.runAsync( () -> emitter.accept(dispatcher), result.cancellableExecutor );

            return result;
        }

        @SuppressWarnings("unchecked")
        public <E> Generator<E> build() {
            if( processor == null ) {
                processor = new BlockingQueueProcessor<>();
            }
            final var cancellableExecutor = CancellableExecutor.of( executor != null ? executor : Generator.DEFAULT_EXECUTOR );
            return new AsyncGeneratorFlow.Generator<>( (Receiver<E>)processor, cancellableExecutor );
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

        static final Executor DEFAULT_EXECUTOR = CompletableFuture::runAsync;

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final Receiver<E> receiver;
        final CancellableExecutor cancellableExecutor;

        public Generator(Receiver<E> receiver ) {
            this(receiver, CancellableExecutor.of(DEFAULT_EXECUTOR));
        }

        Generator(Receiver<E> receiver, CancellableExecutor cancellableExecutor) {
            this.receiver = requireNonNull(receiver, "receiver cannot be null");
            this.cancellableExecutor = requireNonNull(cancellableExecutor, "cancellableExecutor cannot be null");
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
                endData = Data.done(CANCELLED);
            }
            if( isEnded() ) {
                return endData;
            }
            if(executorThread!=null) {
                endData = Data.error(new IllegalStateException("illegal concurrent next() invocation"));
                return endData;
            }
            executorThread = Thread.currentThread();
            try {
                Data<E> value = null;
                while( true ) {
                    value = cancellableExecutor.callUntilCancelled(receiver::waitSync);

                    if (value.isDone() ) {
                        endData = value;
                    }
                    break;
                }
                return value;
            } catch (InterruptedException e) {
                if( !isCancelled() ) {
                    cancel(false);
                    Thread.currentThread().interrupt();
                }
                endData = Data.done(CANCELLED);
                return endData;
            }
            finally {
                executorThread = null;
            }
        }

        /**
         * Once the last element has been returned, a cancel keeps the result.
         */
        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( !super.cancel(mayInterruptIfRunning) ) {
                return false;
            }
            if( endData == null || !endData.isDone() ) {
                cancellableExecutor.cancel(mayInterruptIfRunning);
            }
            return true;
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData ).map( Data::resultValue );
        }
    }

}
