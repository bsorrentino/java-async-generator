package org.bsc.async.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
public class CancellableExecutorTest {

    enum ExecutorType {
        INLINE( Runnable::run ),
        THREAD_POOL(Executors.newCachedThreadPool() );

        final Executor executor;

        ExecutorType( Executor executor ) {
            this.executor = requireNonNull(executor);
        }
    }

    @ParameterizedTest
    @EnumSource(ExecutorType.class)
    void aCallersOwnInterruptSurvivesACancelWithInterruptOfATaskRunningInline( ExecutorType type) throws Exception {
        final var executor = CancellableExecutor.of(type.executor);
        Thread.currentThread().interrupt();

        executor.execute(() -> executor.cancel(true));

        assertTrue(Thread.interrupted());
    }

    @ParameterizedTest
    @EnumSource(ExecutorType.class)
    void anOuterTaskKeepsItsInterruptWhenAnInnerCallOnTheSameThreadEnds( ExecutorType type) throws Exception {
        final var outer = CancellableExecutor.of(type.executor);
        final var inner = CancellableExecutor.of(type.executor);

        final var outerFuture = new CompletableFuture<Boolean>();
        final var innerFuture = new CompletableFuture<Void>();

        outer.execute(() -> {

            inner.execute(() -> {
                try {
                    inner.callUntilCancelled(() -> {
                        inner.cancel(false);
                        outer.cancel(true);
                        innerFuture.complete(null);
                        return null;
                    });
                } catch (InterruptedException ex) {
                    innerFuture.completeExceptionally(ex);
                }
            });

            innerFuture.thenCompose(ignored -> {
                outerFuture.complete(Thread.currentThread().isInterrupted());
                return null;
            });

        });

        final var outerStillInterrupted = outerFuture.get();

        assertTrue(outerStillInterrupted);
        assertFalse(Thread.interrupted());
    }

    @Test
    public void aCallersOwnInterruptSurvivesACancelDuringItsCall() throws Exception {
        final var executor = CancellableExecutor.of(Runnable::run);
        Thread.currentThread().interrupt();

        executor.callUntilCancelled(() -> executor.cancel(false));

        assertTrue(Thread.interrupted());
    }
}
