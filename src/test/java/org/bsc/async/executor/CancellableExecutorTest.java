package org.bsc.async.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
public class CancellableExecutorTest {

    @Test
    public void aCallersOwnInterruptSurvivesACancelWithInterruptOfATaskRunningInline() {
        final var executor = CancellableExecutor.of(Runnable::run);
        Thread.currentThread().interrupt();

        executor.execute(() -> executor.cancel(true));

        assertTrue(Thread.interrupted());
    }

    @Test
    public void anOuterTaskKeepsItsInterruptWhenAnInnerCallOnTheSameThreadEnds() {
        final var outer = CancellableExecutor.of(Runnable::run);
        final var inner = CancellableExecutor.of(Runnable::run);
        final var outerStillInterrupted = new AtomicBoolean();

        outer.execute(() -> {
            inner.execute(() -> {
                try {
                    inner.callUntilCancelled(() -> {
                        inner.cancel(false);
                        outer.cancel(true);
                        return null;
                    });
                } catch (InterruptedException ignored) {
                }
            });
            outerStillInterrupted.set(Thread.currentThread().isInterrupted());
        });

        assertTrue(outerStillInterrupted.get());
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
