package org.bsc.async.executor;

import org.bsc.async.AsyncGenerator;

import java.io.Closeable;
import java.lang.ref.Cleaner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CancellableExecutor implements Executor, AsyncGenerator.IsCancellable, Closeable {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(CancellableExecutor.class.getName());
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
    private static final Cleaner CLEANER = Cleaner.create();

    public static CancellableExecutor newSingleThreadExecutor(String threadName) {
        return new CancellableExecutor(Executors.newSingleThreadExecutor(runnable ->
                new Thread(runnable, "%s[%d]".formatted(threadName, ID_GENERATOR.getAndIncrement()))));
    }

    public static CancellableExecutor of( Executor service) {
        return new CancellableExecutor(service);
    }
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Cleaner.Cleanable cleanable;

    final CloseableExecutor service;

    private CancellableExecutor( Executor service ) {
        final var e = new CloseableExecutor(service);
        this.service = e;

        this.cleanable = CLEANER.register(this, e::cleanUp);
    }


    @Override
    public void execute(Runnable command) {
        log.fine("Executing command in CancellableExecutor");
        service.execute(command);
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        log.fine("Cancelling CancellableExecutor, mayInterruptIfRunning=%s".formatted(mayInterruptIfRunning));
        final var alreadyCancelled = cancelled.getAndSet(true);
        if( alreadyCancelled ) {
            return false;
        }
        if( mayInterruptIfRunning ) {
            service.close();
        }
        return true;
    }

    @Override
    public void close() {
        log.fine("Closing CancellableExecutor");
        service.close();
        cleanable.clean();
    }

}
