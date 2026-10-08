package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class BlockingQueueProcessor<E> implements AsyncGeneratorFlow.Processor<E> {

    class DispatcherImpl implements AsyncGeneratorFlow.Dispatcher<E> {

        @Override
        public void dispatchSync(AsyncGenerator.Data<E> data) throws InterruptedException {
            queue.put(data);
        }

        @Override
        public void dispatchAsync(AsyncGenerator.Data<E> data) {
            queue.add(data);
        }
    }

    class ReceiverImpl implements AsyncGeneratorFlow.Receiver<E> {

        @Override
        public AsyncGenerator.Data<E> waitSync() throws InterruptedException {
            return queue.take();
        }

        @Override
        public Optional<AsyncGenerator.Data<E>> waitAsync() {
            return ofNullable(queue.poll());
        }
    }

    private final  BlockingQueue<AsyncGenerator.Data<E>> queue;
    private final DispatcherImpl dispatcher = new DispatcherImpl();
    private final ReceiverImpl receiver = new ReceiverImpl();

    public BlockingQueueProcessor(BlockingQueue<AsyncGenerator.Data<E>> queue) {
        this.queue = requireNonNull(queue, "queue cannot be null");
    }

    public BlockingQueueProcessor() {
        this( new LinkedBlockingQueue<AsyncGenerator.Data<E>>());
    }

    public final BlockingQueue<AsyncGenerator.Data<E>> queue() {
        return queue;
    }

    @Override
    public AsyncGeneratorFlow.Dispatcher<E> dispatcher() {
        return dispatcher;
    }

    @Override
    public AsyncGeneratorFlow.Receiver<E> receiver() {
        return receiver;
    }
}
