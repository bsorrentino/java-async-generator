package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;
import org.bsc.async.executor.CancellableExecutor;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class BlockingQueueProcessor<E> implements AsyncGeneratorFlow.Processor<E> {

    class DispatcherImpl implements AsyncGeneratorFlow.Dispatcher<E> {

        private final CancellableExecutor cancellableExecutor;

        public DispatcherImpl( Executor executor ) {
            requireNonNull(executor, "executor cannot be null");
            if( executor instanceof CancellableExecutor e ) {
                this.cancellableExecutor = e;
            } else {
                this.cancellableExecutor = CancellableExecutor.of(executor);
            }

        }

        @Override
        public void dispatchSync(AsyncGenerator.Data<E> data) throws InterruptedException {
            if( isCancelled() ) {
                throw new InterruptedException("dispatchSync cancelled");
            }
            queue.put(data);
        }

        @Override
        public void dispatchAsync(AsyncGenerator.Data<E> data) {
            if( isCancelled() ) {
                log.warning("dispatcher already cancelled! operation will be ignored");
                return;
            }
            queue.add(data);

        }

        @Override
        public boolean isCancelled() {
            return cancellableExecutor.isCancelled();
        }

        @Override
        public Executor executor() {
            return cancellableExecutor;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return cancellableExecutor.cancel(mayInterruptIfRunning);
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
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(BlockingQueueProcessor.class.getName());

    private final BlockingQueue<AsyncGenerator.Data<E>> queue;
    private AsyncGeneratorFlow.Dispatcher<E> dispatcher;
    private final AsyncGeneratorFlow.Receiver<E> receiver;

    public BlockingQueueProcessor(BlockingQueue<AsyncGenerator.Data<E>> queue, Executor dispatchExecutor) {
        this.queue = requireNonNull(queue, "queue cannot be null");
        dispatcher = new DispatcherImpl(dispatchExecutor);
        receiver = new ReceiverImpl();
    }
    public BlockingQueueProcessor(BlockingQueue<AsyncGenerator.Data<E>> queue) {
        this(queue, CancellableExecutor.newSingleThreadExecutor("dispatcherExecutor"));
    }
    public BlockingQueueProcessor(Executor dispatchExecutor) {
        this(new LinkedBlockingQueue<>(), dispatchExecutor);
    }
    public BlockingQueueProcessor() {
        this( new LinkedBlockingQueue<>());
    }

    @Override
    public AsyncGeneratorFlow.Dispatcher<E> dispatcher() {
        return dispatcher;
    }

    @Override
    public AsyncGeneratorFlow.Receiver<E> receiver() {
        return receiver;
    }

    @Override
    public void setDispatcherExecutor(Executor executor) {
        dispatcher = new DispatcherImpl(executor);
    }
}
