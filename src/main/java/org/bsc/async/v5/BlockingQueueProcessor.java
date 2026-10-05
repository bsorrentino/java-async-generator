package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public record BlockingQueueProcessor<E>(BlockingQueue<AsyncGenerator.Data<E>> queue) implements AsyncGeneratorFlow.Processor<E> {

    public BlockingQueueProcessor {
        requireNonNull(queue, "queue cannot be null");
    }

    public BlockingQueueProcessor() {
        this( new LinkedBlockingQueue<AsyncGenerator.Data<E>>());
    }

    @Override
    public void dispatchSync(AsyncGenerator.Data<E> data) throws InterruptedException {
        queue.put(data);
    }

    @Override
    public void dispatchAsync(AsyncGenerator.Data<E> data) {
        queue.add(data);
    }

    @Override
    public AsyncGenerator.Data<E> waitSync() throws InterruptedException {
        return queue.take();
    }

    @Override
    public Optional<AsyncGenerator.Data<E>> waitAsync() {
        return ofNullable(queue.poll());
    }
}
