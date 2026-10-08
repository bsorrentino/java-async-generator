package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;
import org.bsc.async.executor.CancellableExecutor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

class CancellableDispatcher<E> implements AsyncGeneratorFlow.Dispatcher<E>, AsyncGeneratorFlow.IsCancellableEx {
    private static final Logger log = Logger.getLogger(CancellableDispatcher.class.getName());
    private static final Executor DEFAULT_EXECUTOR = CompletableFuture::runAsync;

    private final Object listenersLock = new Object();
    private Set<Listener> listeners = new LinkedHashSet<>();
    private final CancellableExecutor executor;

    private boolean completed;
    private boolean cancelledWithInterrupt;

    private final AsyncGeneratorFlow.Dispatcher<E> delegate;

    public CancellableDispatcher(AsyncGeneratorFlow.Dispatcher<E> delegate, Executor executor) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.executor = CancellableExecutor.of(requireNonNullElse(executor, DEFAULT_EXECUTOR));
    }

    void complete() {
        synchronized (listenersLock) {
            if (listeners != null) {
                listeners = null;
                completed = true;
            }
        }
    }

    final boolean isCompleted() {
        return completed;
    }

    public CancellableExecutor executor() {
        return executor;
    }

    @Override
    public final void dispatchSync(AsyncGenerator.Data<E> data) throws InterruptedException {
        if( isCancelled() ) {
            throw new InterruptedException("dispatchSync cancelled");
        }
        executor.callUntilCancelled(() -> {
            delegate.dispatchSync(data);
            return null;
        });

    }

    @Override
    public final void dispatchAsync(AsyncGenerator.Data<E> data) {
        if( !isCancelled() ) {
            delegate.dispatchAsync(data);
        }
    }

    @Override
    public boolean isCancelled() {
        return executor.isCancelled();
    }

    @Override
    public Registration onCancel(Listener listener) {
        requireNonNull(listener, "listener cannot be null");
        final boolean mayInterruptIfRunning;
        synchronized (listenersLock) {
            if (listeners != null) {
                // an entry per registration: closing one of two registrations of the same listener keeps the other
                final Listener entry = listener::cancelled;
                listeners.add(entry);
                return () -> {
                    synchronized (listenersLock) {
                        if (listeners != null) {
                            listeners.remove(entry);
                        }
                    }
                };
            }
            if (isCompleted()) {
                return Registration.noop();
            }
            mayInterruptIfRunning = cancelledWithInterrupt;
        }
        notifyListener(listener, mayInterruptIfRunning);
        return Registration.noop();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final Set<Listener> toNotify;
        synchronized (listenersLock) {
            if (isCompleted()) {
                return true;
            }
            toNotify = listeners;
            listeners = null;
            cancelledWithInterrupt = mayInterruptIfRunning;
        }
        // listeners first: a child running inline on the emitter's thread would otherwise consume the interrupt
        toNotify.forEach(listener -> notifyListener(listener, mayInterruptIfRunning));
        return executor.cancel(mayInterruptIfRunning);
    }


    private static void notifyListener(AsyncGeneratorFlow.IsCancellableEx.Listener listener, boolean mayInterruptIfRunning) {
        try {
            listener.cancelled(mayInterruptIfRunning);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "cancellation listener failed", ex);
        }
    }

}
