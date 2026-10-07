package org.bsc.async.v5;

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

    interface Dispatcher<E> extends  IsCancellable {
        void dispatchSync( Data<E> data ) throws InterruptedException;
        void dispatchAsync( Data<E> data );
        boolean isCancelled();
        Executor executor();
    }

    interface Receiver<E> {
        Data<E> waitSync() throws InterruptedException;
        Optional<Data<E>> waitAsync();
    }

    interface Processor<E>  {
        Dispatcher<E> dispatcher();
        Receiver<E> receiver();
        void setDispatcherExecutor( Executor executor );
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
        @SuppressWarnings("unchecked")
        public <E> Generator<E> build( Consumer<Dispatcher<E>> emitter ) {
            final var result = this.<E>build();

            final var p = (Processor<E>) processor;

            final Runnable emitterTask = () -> {
                if( !result.isCancelled() ) {
                    emitter.accept( p.dispatcher() );
                }
            };

            CompletableFuture.runAsync( emitterTask, p.dispatcher().executor() );

            return result;
        }

        @SuppressWarnings("unchecked")
        public <E> Generator<E> build() {
            if( executor != null ) {
                if( processor != null ) {
                    processor.setDispatcherExecutor(executor);
                }
                else {
                    processor = new BlockingQueueProcessor<>(executor);
                }
            }
            if( processor == null ) {
                processor = new BlockingQueueProcessor<>();
            }

            final var p = (Processor<E>) processor;
            return new AsyncGeneratorFlow.Generator<>( p.receiver(), p.dispatcher() );
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

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final Receiver<E> receiver;
        private final IsCancellable dispatcherExecutor;

        public Generator(Receiver<E> receiver, IsCancellable dispatcherExecutor) {
            this.receiver = requireNonNull(receiver, "receiver cannot be null");
            this.dispatcherExecutor = requireNonNull(dispatcherExecutor, "dispatcherExecutor cannot be null");
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
            if( isCancelled() ) {
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
                    value = receiver.waitSync();

                    if (value.isDone() ) {
                        endData = value;
                    }
                    break;
                }
                return value;
            } catch (InterruptedException e) {
                endData = Data.done(CANCELLED);
                return endData;
            }
            finally {
                executorThread = null;
            }
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            dispatcherExecutor.cancel(mayInterruptIfRunning);
            return super.cancel(mayInterruptIfRunning);
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData ).map( Data::resultValue );
        }
    }


}
