package org.bsc.async;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.bsc.async.AsyncGenerator.*;

/**
 * Represents a queue-based asynchronous generator.
 */
public class AsyncGeneratorQueue    {

    /**
     * Inner class to generate asynchronous elements from the queue.
     *
     * @param <E> the type of elements in the queue
     */
    static class Generator<E> implements AsyncGenerator<E> {

        boolean isEnd = false;
        final java.util.concurrent.BlockingQueue<Data<E>> queue;
        /**
         * Constructs a Generator with the specified queue.
         *
         * @param queue the blocking queue to generate elements from
         */
        Generator(java.util.concurrent.BlockingQueue<Data<E>> queue) {
            this.queue = queue;
        }

        /**
         * Retrieves the next element from the queue asynchronously.
         *
         * @return the next element from the queue
         */
        @Override
        public Data<E> next() {
            while (!isEnd) {
                Data<E> value = queue.poll();
                if (value != null) {
                    if (value.done) {
                        isEnd = true;
                        break;
                    }
                    return value;
                }
            }
            return Data.done();
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
    public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Consumer<Q> consumer) {
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
    public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Consumer<Q> consumer, Executor executor ) {
        Objects.requireNonNull(queue);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(consumer);

        executor.execute( () -> {
            try {
                consumer.accept(queue);
            }
            catch( Throwable ex ) {
                CompletableFuture<E> error = new CompletableFuture<>();
                error.completeExceptionally(ex);
                queue.add( AsyncGenerator.Data.of(error ));
            }
            finally {
                queue.add(Data.done());
            }

        });

        return new Generator<>(queue);
    }

    /**
     * Creates an AsyncGenerator from the provided queue, executor, and consumer.
     *
     * @param <E> the type of elements in the queue
     * @param <Q> the type of blocking queue
     * @param queue the blocking queue to generate elements from
     * @param executor the executor for asynchronous processing
     * @param consumer the consumer for processing elements from the queue
     * @return an AsyncGenerator instance
     * @deprecated use of(Q, Consumer, Executor)
     */
    @Deprecated
    public static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Executor executor, Consumer<Q> consumer) {
        return of(queue, consumer, executor);
    }
}
