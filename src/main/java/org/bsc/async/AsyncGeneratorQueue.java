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
public interface AsyncGeneratorQueue    {

    /**
     * Inner class to generate asynchronous elements from the queue.
     *
     * @param <E> the type of elements in the queue
     */
    class Generator<E> extends BaseCancellable<E> {

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final java.util.concurrent.BlockingQueue<Data<E>> queue;

        /**
         * Constructs a Generator with the specified queue.
         *
         * @param queue the blocking queue to generate elements from
         */
        public Generator(java.util.concurrent.BlockingQueue<Data<E>> queue) {
            this.queue = queue;
        }

        public java.util.concurrent.BlockingQueue<Data<E>> queue() {
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
                Data<E> value = queue.take();
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
                queue.add( AsyncGenerator.Data.of(error));
            }
            finally {
                queue.add(Data.done());
            }

        });

        return new Generator<>(queue);
    }

}
