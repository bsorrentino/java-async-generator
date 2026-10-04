package org.bsc.async.executor;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static java.util.Objects.requireNonNull;

class CloseableExecutor implements Executor, Closeable {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(CloseableExecutor.class.getName());

    final Executor delegate;
    final Map<String,Thread> executionThreadMap = new ConcurrentHashMap<>();;

    public CloseableExecutor(Executor executor) {
        this.delegate = requireNonNull(executor, "executor cannot be null");
    }


    @Override
    public void execute(Runnable command) {
        final Runnable commandWrapper = () -> {
            final var t = Thread.currentThread();
            log.fine("Executing command in CloseableExecutor on thread: %s".formatted(t.getName()));
            executionThreadMap.put(t.getName(), t);
            command.run();
        };

        delegate.execute(commandWrapper);
    }

    private void interruptAllThreads() {
        for( var entry : executionThreadMap.entrySet() ) {
            log.fine("interrupt thread %s".formatted(entry.getKey()));
            entry.getValue().interrupt();
        }
    }

    void cleanUp() {
        if( delegate instanceof ExecutorService executorService ) {
            if( !(executorService.isShutdown() || executorService.isTerminated() ) ) {
                executorService.shutdown();
            }
        }
        else {
            interruptAllThreads();
        }
        executionThreadMap.clear();
    }

    @Override
    public void close() {
        log.fine("Closing CloseableExecutor");
        interruptAllThreads();
        executionThreadMap.clear();
    }
}
