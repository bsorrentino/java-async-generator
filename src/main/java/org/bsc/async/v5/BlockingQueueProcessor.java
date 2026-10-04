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

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(BlockingQueueProcessor.class.getName());

    private final BlockingQueue<AsyncGenerator.Data<E>> queue;
    private CancellableExecutor dispatchExecutor;

    public BlockingQueueProcessor(BlockingQueue<AsyncGenerator.Data<E>> queue, Executor dispatchExecutor) {
        this.queue = requireNonNull(queue, "queue cannot be null");
        setDispatcherExecutor(dispatchExecutor);
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
    public AsyncGenerator.Data<E> waitSync() throws InterruptedException {
        if( isCancelled() ) {
            log.severe("dispatcher already cancelled! operation will be interrupted");
            throw new InterruptedException("waitSync cancelled");
        }
        return queue.take();
    }

    @Override
    public Optional<AsyncGenerator.Data<E>> waitAsync() {
        if( isCancelled() ) {
            return Optional.empty();
        }
        return ofNullable(queue.poll());
    }

    @Override
    public boolean isCancelled() {
        return dispatchExecutor.isCancelled();
    }

    @Override
    public Executor dispatcherExecutor() {
        return dispatchExecutor;
    }

    @Override
    public void setDispatcherExecutor(Executor executor) {
        requireNonNull(executor, "executor cannot be null");
        if( executor instanceof CancellableExecutor cancellableExecutor ) {
            this.dispatchExecutor = cancellableExecutor;
        } else {
            this.dispatchExecutor = CancellableExecutor.of(executor);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return dispatchExecutor.cancel(mayInterruptIfRunning);
    }

}
