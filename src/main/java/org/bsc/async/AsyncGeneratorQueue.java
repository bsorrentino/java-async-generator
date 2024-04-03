package org.bsc.async;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.bsc.async.AsyncGenerator.*;

public class AsyncGeneratorQueue    {

    static class Generator<E> implements AsyncGenerator<E> {

        boolean isEnd = false;
        final java.util.concurrent.BlockingQueue<Data<E>> queue;

        Generator(java.util.concurrent.BlockingQueue<Data<E>> queue) {
            this.queue = queue;
        }

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
    static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Consumer<Q> consumer) {
        return of( queue, commonPool(), consumer);
    }

    static <E, Q extends BlockingQueue<AsyncGenerator.Data<E>>> AsyncGenerator<E> of(Q queue, Executor executor, Consumer<Q> consumer) {
        Objects.requireNonNull(queue);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(consumer);

        executor.execute( () -> {
            try {
                consumer.accept(queue);
            }
            finally {
                queue.add(Data.done());
            }

        });

        return new Generator<>(queue);
    }
}
