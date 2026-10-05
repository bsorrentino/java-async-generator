package org.bsc.async.executor;

import org.bsc.async.AsyncGenerator;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Runs tasks on a borrowed {@link Executor}, which it never shuts down. Once cancelled it skips the tasks not started
 * yet, so a future over one of them never completes.
 */
public class CancellableExecutor implements Executor, AsyncGenerator.IsCancellable {

    @FunctionalInterface
    public interface BlockingCall<T> {
        T call() throws InterruptedException;
    }

    private static final ThreadLocal<Run> CURRENT_RUN = new ThreadLocal<>();

    private final Executor delegate;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Object lock = new Object();
    private final Set<Run> runs = new HashSet<>();
    private final Set<Waiter> waiters = new HashSet<>();

    public static CancellableExecutor of( Executor executor ) {
        return new CancellableExecutor(executor);
    }

    private CancellableExecutor( Executor delegate ) {
        this.delegate = requireNonNull(delegate, "executor cannot be null");
    }

    @Override
    public void execute( Runnable command ) {
        requireNonNull(command, "command cannot be null");
        delegate.execute(() -> run(command));
    }

    private void run( Runnable command ) {
        final var run = new Run(CURRENT_RUN.get());
        synchronized (lock) {
            if( cancelled.get() ) {
                return;
            }
            runs.add(run);
        }
        CURRENT_RUN.set(run);
        try {
            command.run();
        } finally {
            if( run.outer == null ) {
                CURRENT_RUN.remove();
            } else {
                CURRENT_RUN.set(run.outer);
            }
            synchronized (lock) {
                runs.remove(run);
                if( run.interruptRequested && !run.interruptedAtStart ) {
                    clearInterruptUnlessStillRequested();
                }
            }
        }
    }

    /**
     * Any cancel, with or without interrupt, ends the call with an {@link InterruptedException}.
     */
    public <T> T callUntilCancelled( BlockingCall<T> call ) throws InterruptedException {
        final var waiter = new Waiter();
        synchronized (lock) {
            if( cancelled.get() ) {
                throw new InterruptedException("cancelled");
            }
            waiters.add(waiter);
        }
        try {
            return call.call();
        } finally {
            synchronized (lock) {
                waiters.remove(waiter);
                if( waiter.interruptedByCancel || interruptRequestedOnCurrentThread() ) {
                    clearInterruptUnlessStillRequested();
                }
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean cancel( boolean mayInterruptIfRunning ) {
        if( !cancelled.compareAndSet(false, true) ) {
            return false;
        }
        synchronized (lock) {
            if( mayInterruptIfRunning ) {
                for( var run : runs ) {
                    run.interruptRequested = true;
                    run.thread.interrupt();
                }
            }
            for( var waiter : waiters ) {
                if( !waiter.thread.isInterrupted() ) {
                    waiter.interruptedByCancel = true;
                    waiter.thread.interrupt();
                }
            }
        }
        return true;
    }

    /**
     * Clears before checking, so that a cancel(true) racing with this call keeps its interrupt.
     */
    private static void clearInterruptUnlessStillRequested() {
        Thread.interrupted();
        if( interruptRequestedOnCurrentThread() ) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean interruptRequestedOnCurrentThread() {
        for( var run = CURRENT_RUN.get(); run != null; run = run.outer ) {
            if( run.interruptRequested ) {
                return true;
            }
        }
        return false;
    }

    private static final class Run {
        final Thread thread = Thread.currentThread();
        final boolean interruptedAtStart = thread.isInterrupted();
        final Run outer;
        volatile boolean interruptRequested;

        Run( Run outer ) {
            this.outer = outer;
        }
    }

    private static final class Waiter {
        final Thread thread = Thread.currentThread();
        boolean interruptedByCancel;
    }
}
