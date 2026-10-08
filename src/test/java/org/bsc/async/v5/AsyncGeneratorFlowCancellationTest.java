package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.async.AsyncGenerator.Cancellable.CANCELLED;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
public class AsyncGeneratorFlowCancellationTest {

    private static final long WAIT_SECONDS = 5;
    private static final long NEVER_HAPPENS_MILLIS = 300;
    private static final long GC_POLL_MILLIS = 50;
    private static final int RACE_ATTEMPTS = 200;
    private static final int LEAK_ATTEMPTS = 10;
    private static final long SLOW_LISTENER_MILLIS = 200;
    private static final int NESTED_RACE_ATTEMPTS = 1000;
    private static final int OWN_INTERRUPT_ATTEMPTS = 50;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Unlike {@link java.util.concurrent.ThreadPoolExecutor}, does not clear a task's leftover interrupt before
     * running the next.
     */
    static final class NonClearingSingleThreadExecutor implements Executor, AutoCloseable {
        private static final Runnable STOP = () -> {};
        private static final long IDLE_PARK_NANOS = 100_000;
        private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        // polls and parks instead of take(): an interruptible wait would consume the very interrupt the test looks for
        private final Thread worker = new Thread(() -> {
            while (true) {
                final Runnable task = tasks.poll();
                if (task == null) {
                    LockSupport.parkNanos(IDLE_PARK_NANOS);
                    continue;
                }
                if (task == STOP) {
                    return;
                }
                task.run();
            }
        }, "non-clearing-executor");

        NonClearingSingleThreadExecutor() {
            worker.setDaemon(true);
            worker.start();
        }

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        @Override
        public void close() {
            tasks.add(STOP);
        }
    }

    static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public synchronized void execute(Runnable task) {
            tasks.add(task);
        }

        synchronized void runAll() {
            tasks.forEach(Runnable::run);
            tasks.clear();
        }
    }

    /**
     * Implements only the methods a {@link AsyncGeneratorFlow.Processor} had before cancellation support, so a new
     * abstract method fails to compile here.
     */
    static final class UninterruptibleProcessor<E> implements AsyncGeneratorFlow.Processor<E> {

        class DispatcherImpl implements AsyncGeneratorFlow.Dispatcher<E> {
            @Override
            public void dispatchSync(AsyncGenerator.Data<E> data) {
                blocked.countDown();
                boolean interrupted = false;
                while (true) {
                    try {
                        release.await();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                delegate.dispatcher().dispatchAsync(data);
            }

            @Override
            public void dispatchAsync(AsyncGenerator.Data<E> data) {
                delegate.dispatcher().dispatchAsync(data);
            }
        }

        private final BlockingQueueProcessor<E> delegate = new BlockingQueueProcessor<>();
        final CountDownLatch blocked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final DispatcherImpl dispatcher = new DispatcherImpl();

        @Override
        public AsyncGeneratorFlow.Dispatcher<E> dispatcher() {
            return dispatcher;
        }

        @Override
        public AsyncGeneratorFlow.Receiver<E> receiver() {
            return delegate.receiver();
        }

    }

    private static void awaitBlocked(AtomicReference<Thread> thread) {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (thread.get() == null
                || (thread.get().getState() != Thread.State.WAITING && thread.get().getState() != Thread.State.TIMED_WAITING)) {
            if (System.nanoTime() > deadline) {
                fail("thread never blocked: " + thread.get());
            }
            Thread.onSpinWait();
        }
    }

    @Test
    public void cancelWithInterruptInterruptsABlockedEmitter() throws Exception {
        var emitterInterrupted = new CountDownLatch(1);
        var started = new CountDownLatch(1);

        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException e) {
                        emitterInterrupted.countDown();
                    }
                });

        assertTrue(started.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertTrue(generator.cancel(true));

        assertTrue(emitterInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void cancelWithoutInterruptReleasesAnEmitterBlockedOnABoundedQueue() throws Exception {
        var emitterExited = new CountDownLatch(1);
        var emitterThread = new AtomicReference<Thread>();
        var dispatchReleased = new AtomicBoolean();
        var interruptLeftAfterRelease = new AtomicBoolean(true);

        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(new BlockingQueueProcessor<String>(new ArrayBlockingQueue<>(1)))
                .<String>build(dispatcher -> {
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        emitterThread.set(Thread.currentThread());
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e2")));
                    } catch (InterruptedException e) {
                        dispatchReleased.set(true);
                        interruptLeftAfterRelease.set(Thread.currentThread().isInterrupted());
                    } finally {
                        emitterExited.countDown();
                    }
                });

        awaitBlocked(emitterThread);
        assertTrue(generator.cancel(false));

        assertTrue(emitterExited.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertTrue(dispatchReleased.get());
        assertFalse(interruptLeftAfterRelease.get());
    }

    @Test
    public void cancelWithoutInterruptLeavesTheEmittersLaterWorkUninterrupted() throws Exception {
        var processor = new UninterruptibleProcessor<String>();
        var laterWorkInterrupted = new AtomicBoolean(true);
        var laterWorkDone = new CountDownLatch(1);

        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(processor)
                .<String>build(dispatcher -> {
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        Thread.sleep(10);
                        laterWorkInterrupted.set(false);
                    } catch (InterruptedException e) {
                        laterWorkInterrupted.set(true);
                    } finally {
                        laterWorkDone.countDown();
                    }
                });

        assertTrue(processor.blocked.await(WAIT_SECONDS, TimeUnit.SECONDS));
        generator.cancel(false);
        processor.release.countDown();

        assertTrue(laterWorkDone.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertFalse(laterWorkInterrupted.get());
    }

    @Test
    public void cancelWithInterruptStillInterruptsTheEmittersNextBlockingCallAfterADroppedDispatch() throws Exception {
        var emitterThread = new AtomicReference<Thread>();
        var nextCallInterrupted = new CountDownLatch(1);

        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(new BlockingQueueProcessor<String>(new ArrayBlockingQueue<>(1)))
                .<String>build(dispatcher -> {
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        emitterThread.set(Thread.currentThread());
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e2")));
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    } catch (InterruptedException e) {
                        nextCallInterrupted.countDown();
                    }
                });

        awaitBlocked(emitterThread);
        generator.cancel(true);

        assertTrue(nextCallInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void nextAfterCancelDoesNotWaitForTheEmitter() throws Exception {
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException ignored) {
                    }
                });

        generator.cancel(false);

        var next = CompletableFuture.supplyAsync(generator::next, executor).get(WAIT_SECONDS, TimeUnit.SECONDS);
        assertTrue(next.isDone());
        assertEquals(CANCELLED, next.resultValue());
        assertEquals(CANCELLED, generator.resultValue().orElseThrow());
    }

    @Test
    public void cancelWithoutInterruptReleasesAConsumerBlockedInNext() throws Exception {
        var consumerThread = new AtomicReference<Thread>();
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    while (!dispatcher.isCancelled()) {
                        Thread.onSpinWait();
                    }
                });

        var consumer = CompletableFuture.supplyAsync(() -> {
            consumerThread.set(Thread.currentThread());
            var data = generator.next();
            return List.of(data.resultValue(), Thread.currentThread().isInterrupted());
        }, executor);
        awaitBlocked(consumerThread);
        generator.cancel(false);

        assertEquals(List.of(CANCELLED, false), consumer.get(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void aConsumerInterruptedFromElsewhereKeepsItsInterruptAndCancelsTheGenerator() throws Exception {
        var consumerThread = new AtomicReference<Thread>();
        var emitterStopped = new CountDownLatch(1);
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    while (!dispatcher.isCancelled()) {
                        Thread.onSpinWait();
                    }
                    emitterStopped.countDown();
                });

        var consumer = CompletableFuture.supplyAsync(() -> {
            consumerThread.set(Thread.currentThread());
            var data = generator.next();
            return List.of(data.resultValue(), Thread.currentThread().isInterrupted());
        }, executor);
        awaitBlocked(consumerThread);
        consumerThread.get().interrupt();

        assertEquals(List.of(CANCELLED, true), consumer.get(WAIT_SECONDS, TimeUnit.SECONDS));
        assertTrue(generator.isCancelled());
        assertTrue(emitterStopped.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void cancelRacingAConsumerEnteringNextNeverHangs() throws Exception {
        for (int attempt = 0; attempt < RACE_ATTEMPTS; attempt++) {
            var generator = AsyncGeneratorFlow.builder()
                    .executor(executor)
                    .<String>build(dispatcher -> {
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException ignored) {
                        }
                    });

            var next = CompletableFuture.supplyAsync(generator::next, executor);
            generator.cancel(false);

            var data = next.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertEquals(CANCELLED, data.resultValue(), "attempt " + attempt);
            generator.cancel(true);
        }
    }

    @Test
    public void cancelBeforeTheEmitterStartsSkipsIt() {
        var manualExecutor = new ManualExecutor();
        var emitterRan = new AtomicBoolean();

        var generator = AsyncGeneratorFlow.builder()
                .executor(manualExecutor)
                .<String>build(dispatcher -> emitterRan.set(true));

        generator.cancel(false);
        manualExecutor.runAll();

        assertFalse(emitterRan.get());
        assertEquals(CANCELLED, generator.next().resultValue());
    }

    @Test
    public void aCompletedStreamKeepsItsResultWhenCancelledAfterwards() {
        var generator = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .<String>build(dispatcher -> {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("e1")));
                    dispatcher.dispatchAsync(AsyncGenerator.Data.done("END"));
                });

        assertEquals(List.of("e1"), generator.stream().toList());
        generator.cancel(true);

        assertEquals("END", generator.next().resultValue());
        assertEquals("END", generator.resultValue().orElseThrow());
    }

    @Test
    public void anEmitterStopsWhenItSeesTheCancellationAndLaterDispatchesAreDropped() throws Exception {
        var processor = new BlockingQueueProcessor<String>();
        var firstDispatched = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        var stoppedOnCancel = new CountDownLatch(1);

        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(processor)
                .<String>build(dispatcher -> {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("e1")));
                    firstDispatched.countDown();
                    try {
                        resume.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if (dispatcher.isCancelled()) {
                        dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("late")));
                        stoppedOnCancel.countDown();
                    }
                });

        assertTrue(firstDispatched.await(WAIT_SECONDS, TimeUnit.SECONDS));
        generator.cancel(false);
        resume.countDown();

        assertTrue(stoppedOnCancel.await(WAIT_SECONDS, TimeUnit.SECONDS));
        assertEquals(1, processor.queue().size());
        assertEquals(CANCELLED, generator.next().resultValue());
    }

    @Test
    public void aProcessorSharedByTwoGeneratorsIsNotCancelledWithOne() {
        var processor = new BlockingQueueProcessor<String>();

        try( var cancelled = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(processor)
                .<String>build();
            var other = new AsyncGeneratorFlow.Generator<>(processor.receiver())) {

            cancelled.cancel(true);
            processor.dispatcher().dispatchAsync(AsyncGenerator.Data.done("END"));

            assertEquals("END", other.next().resultValue());
        }
    }

    @Test
    public void cancelWithInterruptDoesNotInterruptAnotherTaskOfTheSameExecutor() throws Exception {
        var singleThread = Executors.newSingleThreadExecutor();
        try {
            var generator = AsyncGeneratorFlow.builder()
                    .executor(singleThread)
                    .<String>build(dispatcher -> dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("e1"))));
            var otherTaskStarted = new CountDownLatch(1);
            var otherTask = CompletableFuture.supplyAsync(() -> {
                otherTaskStarted.countDown();
                try {
                    Thread.sleep(NEVER_HAPPENS_MILLIS);
                    return false;
                } catch (InterruptedException e) {
                    return true;
                }
            }, singleThread);
            assertTrue(otherTaskStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));

            generator.cancel(true);

            assertFalse(otherTask.get(WAIT_SECONDS, TimeUnit.SECONDS));
        } finally {
            singleThread.shutdownNow();
        }
    }

    @Test
    public void cancelAfterTheLastElementLeavesTheEmittersCleanupUninterrupted() throws Exception {
        var cleanup = new CountDownLatch(1);
        var cleanupInterrupted = new CompletableFuture<Boolean>();
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.done("END"));
                    try {
                        cleanup.await();
                        cleanupInterrupted.complete(false);
                    } catch (InterruptedException e) {
                        cleanupInterrupted.complete(true);
                    }
                });

        assertEquals("END", generator.next().resultValue());
        generator.cancel(true);
        cleanup.countDown();

        assertFalse(cleanupInterrupted.get(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void cancelAfterAConcurrentNextStillReleasesTheFirstConsumer() throws Exception {
        var consumerThread = new AtomicReference<Thread>();
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    while (!dispatcher.isCancelled()) {
                        Thread.onSpinWait();
                    }
                });
        var firstConsumer = CompletableFuture.supplyAsync(() -> {
            consumerThread.set(Thread.currentThread());
            return generator.next().resultValue();
        }, executor);
        awaitBlocked(consumerThread);
        assertTrue(generator.next().isError());

        generator.cancel(false);

        assertEquals(CANCELLED, firstConsumer.get(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void theCallersExecutorOutlivesItsGenerators() throws Exception {
        var callersExecutor = Executors.newFixedThreadPool(1);
        try {
            var generator = AsyncGeneratorFlow.builder()
                    .executor(callersExecutor)
                    .<String>build(dispatcher -> dispatcher.dispatchAsync(AsyncGenerator.Data.done("END")));
            assertEquals("END", generator.next().resultValue());
            generator.cancel(true);
            generator.close();
            var collected = new WeakReference<>(generator);
            generator = null;

            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
            while (collected.get() != null && System.nanoTime() < deadline) {
                System.gc();
                Thread.sleep(GC_POLL_MILLIS);
            }
            // gives a Cleaner registered with the generator time to run
            Thread.sleep(NEVER_HAPPENS_MILLIS);

            assertFalse(callersExecutor.isShutdown());
        } finally {
            callersExecutor.shutdownNow();
        }
    }

    @Test
    public void anInterruptSentByCancelDoesNotLeakIntoTheExecutorsNextTask() throws Exception {
        try (var nonClearing = new NonClearingSingleThreadExecutor()) {
            for (int attempt = 0; attempt < LEAK_ATTEMPTS; attempt++) {
                var started = new CountDownLatch(1);
                var generator = AsyncGeneratorFlow.builder()
                        .executor(nonClearing)
                        .<String>build(dispatcher -> {
                            started.countDown();
                            while (!Thread.currentThread().isInterrupted()) {
                                Thread.onSpinWait();
                            }
                        });

                assertTrue(started.await(WAIT_SECONDS, TimeUnit.SECONDS));
                generator.cancel(true);

                var nextTaskInterrupted = CompletableFuture
                        .supplyAsync(() -> Thread.currentThread().isInterrupted(), nonClearing)
                        .get(WAIT_SECONDS, TimeUnit.SECONDS);
                assertFalse(nextTaskInterrupted, "attempt " + attempt);
            }
        }
    }

    @Test
    public void cancellingTheParentCancelsTheChildWithTheSameInterruptFlag() throws Exception {
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});
        var childInterrupted = new CountDownLatch(1);
        var childStarted = new CountDownLatch(1);

        var child = AsyncGeneratorFlow.builder()
                .executor(executor)
                .cancelledBy(parent)
                .<String>build(dispatcher -> {
                    childStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException e) {
                        childInterrupted.countDown();
                    }
                });

        assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        parent.cancel(true);

        assertTrue(child.isCancelled());
        assertTrue(childInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void cancellingTheParentWithoutInterruptDoesNotInterruptTheChild() throws Exception {
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});
        var childInterrupted = new CountDownLatch(1);
        var childStarted = new CountDownLatch(1);

        var child = AsyncGeneratorFlow.builder()
                .executor(executor)
                .cancelledBy(parent)
                .<String>build(dispatcher -> {
                    childStarted.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException e) {
                        childInterrupted.countDown();
                    }
                });

        assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        parent.cancel(false);

        assertTrue(child.isCancelled());
        assertFalse(childInterrupted.await(NEVER_HAPPENS_MILLIS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void aChildOfAnAlreadyCancelledParentNeverRunsItsEmitter() {
        var parent = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .<String>build(dispatcher -> {});
        parent.cancel(false);
        var emitterRan = new AtomicBoolean();

        var child = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .cancelledBy(parent)
                .<String>build(dispatcher -> emitterRan.set(true));

        assertTrue(child.isCancelled());
        assertFalse(emitterRan.get());
    }

    @Test
    public void cancellingACompletedParentLeavesItsLinkedGeneratorsRunning() throws Exception {
        var parent = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .<String>build(dispatcher -> dispatcher.dispatchAsync(AsyncGenerator.Data.done("END")));
        assertEquals("END", parent.next().resultValue());
        var childStarted = new CountDownLatch(1);
        var childRelease = new CountDownLatch(1);
        var linkedBefore = AsyncGeneratorFlow.builder()
                .executor(executor)
                .cancelledBy(parent)
                .<String>build(dispatcher -> {
                    childStarted.countDown();
                    try {
                        childRelease.await();
                    } catch (InterruptedException ignored) {
                    }
                });
        assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));

        assertTrue(parent.cancel(true));
        var lateListenerCalled = new AtomicBoolean();
        parent.onCancel(mayInterrupt -> lateListenerCalled.set(true));
        var linkedAfter = AsyncGeneratorFlow.builder()
                .executor(executor)
                .cancelledBy(parent)
                .<String>build(dispatcher -> dispatcher.dispatchAsync(AsyncGenerator.Data.done("END")));

        assertFalse(linkedBefore.isCancelled());
        assertFalse(linkedAfter.isCancelled());
        assertFalse(lateListenerCalled.get());
        assertEquals("END", parent.resultValue().orElseThrow());
        childRelease.countDown();
    }

    @Test
    public void aParentCancelledBeforeItCompletesStillCancelsLateLinks() {
        var parent = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .<String>build(dispatcher -> {});
        parent.cancel(false);
        assertEquals(CANCELLED, parent.next().resultValue());

        var child = AsyncGeneratorFlow.builder()
                .executor(executor)
                .cancelledBy(parent)
                .<String>build(dispatcher -> {});

        assertTrue(child.isCancelled());
    }

    @Test
    public void aChildConsumedToItsEndIsNoLongerLinkedToItsParent() {
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});

        var child = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .cancelledBy(parent)
                .<String>build(dispatcher -> dispatcher.dispatchAsync(AsyncGenerator.Data.done("END")));
        assertEquals("END", child.next().resultValue());

        parent.cancel(true);

        assertFalse(child.isCancelled());
        assertEquals("END", child.resultValue().orElseThrow());
    }

    @Test
    public void aChildWhoseEmitterFinishedButIsStillBeingConsumedIsCancelledWithItsParent() {
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});

        var child = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .cancelledBy(parent)
                .<String>build(dispatcher -> {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("e1")));
                    dispatcher.dispatchAsync(AsyncGenerator.Data.done("END"));
                });

        parent.cancel(false);

        assertTrue(child.isCancelled());
        assertEquals(CANCELLED, child.next().resultValue());
    }

    @Test
    public void cancellingAParentWhoseEmitterConsumesTheChildInterruptsTheChild() throws Exception {
        for (int attempt = 0; attempt < NESTED_RACE_ATTEMPTS; attempt++) {
            var childStarted = new CountDownLatch(1);
            var childInterrupted = new CountDownLatch(1);
            var parent = AsyncGeneratorFlow.builder()
                    .executor(executor)
                    .<String>build(dispatcher -> {
                        var child = AsyncGeneratorFlow.builder()
                                .executor(executor)
                                .cancelledBy(dispatcher)
                                .<String>build(childDispatcher -> {
                                    childStarted.countDown();
                                    try {
                                        new CountDownLatch(1).await();
                                    } catch (InterruptedException e) {
                                        childInterrupted.countDown();
                                    }
                                });
                        child.next();
                    });

            assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
            parent.cancel(true);

            assertTrue(childInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS), "attempt " + attempt);
        }
    }

    @Test
    public void aProcessorModeChildConsumedToItsEndIsNoLongerLinkedToItsParent() {
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});
        var processor = new BlockingQueueProcessor<String>();

        var child = AsyncGeneratorFlow.builder()
                .processor(processor)
                .cancelledBy(parent)
                .<String>build();
        processor.dispatcher().dispatchAsync(AsyncGenerator.Data.done("END"));
        assertEquals("END", child.next().resultValue());

        parent.cancel(true);

        assertFalse(child.isCancelled());
    }

    @Test
    public void aParentEmitterConsumingALinkedChildStillSeesItsOwnInterrupt() throws Exception {
        for (int attempt = 0; attempt < OWN_INTERRUPT_ATTEMPTS; attempt++) {
            var childStarted = new CountDownLatch(1);
            var parentInterruptedAfterChild = new CountDownLatch(1);
            var parent = AsyncGeneratorFlow.builder()
                    .executor(executor)
                    .<String>build(dispatcher -> {
                        var child = AsyncGeneratorFlow.builder()
                                .executor(executor)
                                .cancelledBy(dispatcher)
                                .<String>build(childDispatcher -> {
                                    childStarted.countDown();
                                    try {
                                        new CountDownLatch(1).await();
                                    } catch (InterruptedException ignored) {
                                    }
                                });
                        child.next();
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                        } catch (InterruptedException e) {
                            parentInterruptedAfterChild.countDown();
                        }
                    });

            assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
            parent.cancel(true);

            assertTrue(parentInterruptedAfterChild.await(WAIT_SECONDS, TimeUnit.SECONDS), "attempt " + attempt);
        }
    }

    @Test
    public void aParentEmitterRunningALinkedChildInlineStillSeesItsOwnInterrupt() throws Exception {
        for (boolean childRestoresInterrupt : new boolean[]{false, true}) {
            for (int attempt = 0; attempt < OWN_INTERRUPT_ATTEMPTS; attempt++) {
                var childStarted = new CountDownLatch(1);
                var parentInterruptedAfterChild = new CountDownLatch(1);
                var parent = AsyncGeneratorFlow.builder()
                        .executor(executor)
                        .<String>build(dispatcher -> {
                            AsyncGeneratorFlow.builder()
                                    .executor(Runnable::run)
                                    .cancelledBy(dispatcher)
                                    .<String>build(childDispatcher -> {
                                        childStarted.countDown();
                                        try {
                                            new CountDownLatch(1).await();
                                        } catch (InterruptedException e) {
                                            if (childRestoresInterrupt) {
                                                Thread.currentThread().interrupt();
                                            }
                                        }
                                    });
                            try {
                                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                            } catch (InterruptedException e) {
                                parentInterruptedAfterChild.countDown();
                            }
                        });

                assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
                parent.cancel(true);

                assertTrue(parentInterruptedAfterChild.await(WAIT_SECONDS, TimeUnit.SECONDS),
                        "childRestoresInterrupt=" + childRestoresInterrupt + " attempt " + attempt);
            }
        }
    }

    @Test
    public void aParentEmitterKeepsItsInterruptWhenAnInlineChildEndsBeforeItsCancelArrives() throws Exception {
        var childStarted = new CountDownLatch(1);
        var parentInterruptedAfterChild = new CountDownLatch(1);
        var parent = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    dispatcher.onCancel(mayInterrupt -> {
                        try {
                            Thread.sleep(SLOW_LISTENER_MILLIS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    AsyncGeneratorFlow.builder()
                            .executor(Runnable::run)
                            .cancelledBy(dispatcher)
                            .<String>build(childDispatcher -> {
                                childStarted.countDown();
                                try {
                                    new CountDownLatch(1).await();
                                } catch (InterruptedException ignored) {
                                }
                            });
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    } catch (InterruptedException e) {
                        parentInterruptedAfterChild.countDown();
                    }
                });

        assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        parent.cancel(true);

        assertTrue(parentInterruptedAfterChild.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void aChildReleasesItsParentLinkWhenClosedOrRejected() {
        var parent = new CountingToken();

        AsyncGeneratorFlow.builder()
                .cancelledBy(parent)
                .<String>build()
                .close();
        assertThrows(RejectedExecutionException.class, () -> AsyncGeneratorFlow.builder()
                .executor(command -> { throw new RejectedExecutionException(); })
                .cancelledBy(parent)
                .<String>build(dispatcher -> {}));

        assertEquals(2, parent.registered.get());
        assertEquals(0, parent.open.get());
    }

    static final class CountingToken implements AsyncGeneratorFlow.IsCancellableEx {
        final AtomicInteger registered = new AtomicInteger();
        final AtomicInteger open = new AtomicInteger();

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public Registration onCancel(Listener listener) {
            registered.incrementAndGet();
            open.incrementAndGet();
            return open::decrementAndGet;
        }
    }

    @Test
    public void anInterruptHandedToTheParentDoesNotLeakIntoTheExecutorsNextTask() throws Exception {
        try (var nonClearing = new NonClearingSingleThreadExecutor()) {
            for (int attempt = 0; attempt < LEAK_ATTEMPTS; attempt++) {
                var childStarted = new CountDownLatch(1);
                var parent = AsyncGeneratorFlow.builder()
                        .executor(nonClearing)
                        .<String>build(dispatcher -> {
                            AsyncGeneratorFlow.builder()
                                    .executor(Runnable::run)
                                    .cancelledBy(dispatcher)
                                    .<String>build(childDispatcher -> {
                                        childStarted.countDown();
                                        while (!Thread.currentThread().isInterrupted()) {
                                            Thread.onSpinWait();
                                        }
                                    });
                            while (!Thread.currentThread().isInterrupted()) {
                                Thread.onSpinWait();
                            }
                        });

                assertTrue(childStarted.await(WAIT_SECONDS, TimeUnit.SECONDS));
                parent.cancel(true);

                var nextTaskInterrupted = CompletableFuture
                        .supplyAsync(() -> Thread.currentThread().isInterrupted(), nonClearing)
                        .get(WAIT_SECONDS, TimeUnit.SECONDS);
                assertFalse(nextTaskInterrupted, "attempt " + attempt);
            }
        }
    }

    @Test
    public void linkingToADispatcherNotTiedToAGeneratorFails() {
        var processor = new BlockingQueueProcessor<String>();

        assertThrows(UnsupportedOperationException.class, () -> AsyncGeneratorFlow.builder()
                .cancelledBy(processor.dispatcher())
                .<String>build());
    }

    @Test
    public void cancellationListeners() {
        var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {});
        var calls = new CopyOnWriteArrayList<String>();
        var registeredCalls = new AtomicInteger();
        AsyncGeneratorFlow.IsCancellableEx.Listener sharedListener = mayInterrupt -> calls.add("shared:" + mayInterrupt);

        generator.onCancel(mayInterrupt -> registeredCalls.incrementAndGet());
        generator.onCancel(mayInterrupt -> { throw new IllegalStateException("listener failure"); });
        var firstShared = generator.onCancel(sharedListener);
        generator.onCancel(sharedListener);
        firstShared.close();
        firstShared.close();
        generator.onCancel(mayInterrupt -> calls.add("removed")).close();

        assertTrue(generator.cancel(true));
        assertFalse(generator.cancel(true));
        generator.onCancel(mayInterrupt -> calls.add("late:" + mayInterrupt));
        assertDoesNotThrow(() -> generator.onCancel(mayInterrupt -> { throw new IllegalStateException("late failure"); }));

        assertEquals(1, registeredCalls.get());
        assertEquals(List.of("shared:true", "late:true"), calls);
    }
}
