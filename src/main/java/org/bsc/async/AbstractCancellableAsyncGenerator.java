package org.bsc.async;

import java.util.concurrent.atomic.AtomicBoolean;


public abstract class AbstractCancellableAsyncGenerator<E> implements AsyncGenerator.Cancellable<E> {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean cancel() {
        return cancelled.compareAndSet( false, true );
    }

}
