package org.bsc.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.bsc.async.AsyncGenerator.*;

/**
 * Represents a queue-based asynchronous generator.
 */
public interface AsyncGeneratorQueue    {


    @FunctionalInterface
    interface TakeElementFunction<E> {
        Data<E> apply( BlockingQueue<Data<E>> queue ) throws InterruptedException;
    }

    class Builder<E> {
        private BlockingQueue<Data<E>> queue;
        private TakeElementFunction<E> takeElementFunction;

        public Builder<E> blockingQueue( BlockingQueue<Data<E>> queue ) {
            this.queue = queue;
            return this;
        }

        public Builder<E> takeElementFunction( TakeElementFunction<E> takeElementFunction ) {
            this.takeElementFunction = takeElementFunction;
            return this;
        }

        public Builder<E> takeElementFunctionUsingPoll( long timeout, TimeUnit unit ) {
            this.takeElementFunction = (queue) -> {
                Data<E> result = null;
                do {
                    result = queue.poll(timeout, unit);
                } while( result == null );

                return result;
            };
            return this;
        }

        public Generator<E> build() {
            return new Generator<>( queue, ofNullable(takeElementFunction)
                                            .orElseGet( () -> BlockingQueue::take) );
        }
    }

    static <E> Builder<E> builder() {
        return new Builder<>();
    }

    /**
     * Inner class to generate asynchronous elements from the queue.
     *
     * @param <E> the type of elements in the queue
     */
    class Generator<E> extends BaseCancellable<E> {

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final java.util.concurrent.BlockingQueue<Data<E>> queue;
        private final TakeElementFunction<E> takeElementFunction;


        /**
         * Constructs a Generator with the specified queue and takeElement function.
         *
         * @param queue the blocking queue to generate elements from
         * @param takeElementFunction the function to take elements from the queue
         */
        public Generator( BlockingQueue<Data<E>> queue, TakeElementFunction<E> takeElementFunction ) {
            this.queue = requireNonNull(queue, "queue cannot be null!");
            this.takeElementFunction = requireNonNull( takeElementFunction, "takeElementFunction cannot be null!");
        }

        /**
         * Constructs a Generator with the specified queue.
         *
         * @param queue the blocking queue to generate elements from
         */
        public Generator( BlockingQueue<Data<E>> queue ) {
            this( queue, BlockingQueue::take);
        }

        public BlockingQueue<Data<E>> queue() {
            return queue;
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
            if( isEnded() ) {
                return endData;
            }
            if(executorThread!=null) {
                endData = Data.error(new IllegalStateException("illegal concurrent next() invocation"));
                return endData;
            }
            executorThread = Thread.currentThread();
            try {
                Data<E> value = takeElementFunction.apply(queue);
                if (value.isDone()) {
                    endData = value;
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
            if( super.cancel(mayInterruptIfRunning) ) {
                if( executorThread != null ) {
                    executorThread.interrupt();
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Creates an AsyncGenerator from the provided blocking queue and consumer.
     *
     * @param <E> the type of elements in the queue
     * @param <Q> the type of blocking queue
     * @param queue the blocking queue to generate elements from
     * @param consumer the consumer for processing elements from the queue
     * @return an AsyncGenerator instance
     */
    static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Consumer<Q> consumer) {
        return of( queue, consumer, commonPool() );
    }

    /**
     * Creates an AsyncGenerator from the provided queue, executor, and consumer.
     *
     * @param <E> the type of elements in the queue
     * @param <Q> the type of blocking queue
     * @param queue the blocking queue to generate elements from
     * @param consumer the consumer for processing elements from the queue
     * @param executor the executor for asynchronous processing
     * @return an AsyncGenerator instance
     */
    static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Consumer<Q> consumer, Executor executor ) {
        requireNonNull(queue);
        requireNonNull(executor);
        requireNonNull(consumer);

        executor.execute( () -> {
            try {
                consumer.accept(queue);
            }
            catch( Throwable ex ) {
                CompletableFuture<E> error = new CompletableFuture<>();
                error.completeExceptionally(ex);
                queue.add( AsyncGenerator.Data.of(error));
            }
            finally {
                queue.add(Data.done());
            }

        });

        return new Generator<>(queue);
    }

}
