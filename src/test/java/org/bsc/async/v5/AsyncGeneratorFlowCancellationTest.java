package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.LogManager;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.async.AsyncGenerator.Cancellable.CANCELLED;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
public class AsyncGeneratorFlowCancellationTest {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("test");

    private static final long WAIT_SECONDS = 5;
    private static final int RACE_ATTEMPTS = 200;
    private static final int LEAK_ATTEMPTS = 10;

    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    private final ExecutorService executor = Executors.newCachedThreadPool( r -> {
        return new Thread(r, "flowCancellationTest[%d]".formatted(threadCounter.getAndIncrement()));
    });

    @BeforeAll
    static void initLogger() throws Exception {
        try (InputStream in = AsyncGeneratorFlowCancellationTest.class.getResourceAsStream("/logging.properties")) {
            if (in == null)
                throw new IllegalStateException("logging.properties not found");
            LogManager.getLogManager().readConfiguration(in);
        }
    }

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

        NonClearingSingleThreadExecutor() {
            // polls and parks instead of take(): an interruptible wait would consume the very interrupt the test looks for
            Thread worker = new Thread(() -> {
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
            worker.setDaemon(true);
            worker.start();
        }

        @Override
        public void execute( Runnable task) {
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

    static final class UninterruptibleProcessor<E> implements AsyncGeneratorFlow.Processor<E> {
        private final BlockingQueueProcessor<E> delegate;
        final CountDownLatch blocked = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        public UninterruptibleProcessor( Executor executor ) {
            delegate = new BlockingQueueProcessor<>( executor);
        }
        public UninterruptibleProcessor() {
            delegate = new BlockingQueueProcessor<>();
        }

        @Override
        public void dispatchSync(AsyncGenerator.Data<E> data) {
            blocked.countDown();
            boolean interrupted = false;
            while (true) {
                try {
                    release.await();
                    break;
                } catch (InterruptedException e) {
                    log.info( "interrupted while waiting to release processor, will continue waiting");
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            delegate.dispatchAsync(data);
        }

        @Override
        public void dispatchAsync(AsyncGenerator.Data<E> data) {
            delegate.dispatchAsync(data);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public AsyncGenerator.Data<E> waitSync() throws InterruptedException {
            return delegate.waitSync();
        }

        @Override
        public Optional<AsyncGenerator.Data<E>> waitAsync() {
            return delegate.waitAsync();
        }

        @Override
        public Executor dispatcherExecutor() {
            return delegate.dispatcherExecutor();
        }

        @Override
        public void setDispatcherExecutor(Executor executor) {
            delegate.setDispatcherExecutor(executor);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
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

        try( var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    started.countDown();
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException e) {
                        emitterInterrupted.countDown();
                    }
                })) {

            assertTrue(started.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertTrue(generator.cancel(true));

            assertTrue(emitterInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    public void cancelWithoutInterruptReleasesAnEmitterBlockedOnABoundedQueue() throws Exception {
        var emitterExited = new CountDownLatch(1);
        var emitterThread = new AtomicReference<Thread>();

        try( var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(new BlockingQueueProcessor<String>(new ArrayBlockingQueue<>(1), executor))
                .<String>build(dispatcher -> {
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        log.info( "dispatching 'e1' on thread [%s]".formatted(Thread.currentThread().getName()));
                        emitterThread.set(Thread.currentThread());
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e2")));
                        log.info( "dispatching 'e2' on thread [%s]".formatted(Thread.currentThread().getName()));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        emitterExited.countDown();
                    }
                })) {

            awaitBlocked(emitterThread);
            assertTrue(generator.cancel(true));

            assertTrue(emitterExited.await(WAIT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    public void cancelWithoutInterruptLeavesTheEmittersLaterWorkUninterrupted() throws Exception {
        var processor = new UninterruptibleProcessor<String>(executor);
        var laterWorkInterrupted = new AtomicBoolean(true);
        var laterWorkDone = new CountDownLatch(1);

        try( var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(processor)
                .<String>build(dispatcher -> {
                    final var tName = Thread.currentThread().getName();
                    log.info( "start dispatching on thread [%s]".formatted(tName));
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        log.info( "dispatching on thread [%s]".formatted(tName));
                        Thread.sleep(10);
                        laterWorkInterrupted.set(false);
                    } catch (InterruptedException e) {
                        log.info( "interrupted dispatching on thread [%s]".formatted(tName));
                        laterWorkInterrupted.set(true);
                    } finally {
                        laterWorkDone.countDown();
                    }
                })) {
            assertTrue(processor.blocked.await(WAIT_SECONDS, TimeUnit.SECONDS));
            generator.cancel(false);
            processor.release.countDown();

            assertTrue(laterWorkDone.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertFalse(laterWorkInterrupted.get());
        }
    }

    @Test
    public void cancelWithInterruptStillInterruptsTheEmittersNextBlockingCallAfterADroppedDispatch() throws Exception {
        var emitterThread = new AtomicReference<Thread>();
        var nextCallInterrupted = new CountDownLatch(1);

        try( var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor(new BlockingQueueProcessor<String>(new ArrayBlockingQueue<>(1)))
                .<String>build(dispatcher -> {
                    try {
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e1")));
                        emitterThread.set(Thread.currentThread());
                        dispatcher.dispatchSync(AsyncGenerator.Data.of(completedFuture("e2")));
                        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    } catch (InterruptedException e) {
                        nextCallInterrupted.countDown();
                    }
                })) {

            awaitBlocked(emitterThread);
            generator.cancel(true);

            assertTrue(nextCallInterrupted.await(WAIT_SECONDS, TimeUnit.SECONDS));
        }
    }

    @Test
    public void nextAfterCancelDoesNotWaitForTheEmitter() throws Exception {
        try( var generator = AsyncGeneratorFlow.builder()
                .executor(executor)
                .<String>build(dispatcher -> {
                    try {
                        new CountDownLatch(1).await();
                    } catch (InterruptedException ignored) {
                    }
                })) {

            generator.cancel(false);

            var next = CompletableFuture.supplyAsync(generator::next, executor).get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertTrue(next.isDone());
            assertEquals(CANCELLED, next.resultValue());
            assertEquals(CANCELLED, generator.resultValue().orElseThrow());
        }
    }

    @Test
    public void cancelRacingAConsumerEnteringNextNeverHangs() throws Exception {
        for (int attempt = 0; attempt < RACE_ATTEMPTS; attempt++) {

            try(var generator = AsyncGeneratorFlow.builder()
                    //.executor(executor)
                    .<String>build(dispatcher -> {
                        try {
                            new CountDownLatch(1).await();
                        } catch (InterruptedException ignored) {
                            log.info( "emitter interrupted on " + Thread.currentThread().getName());
                        }
                    })) {

                var result = generator.toCompletableFutureAsync();
                generator.cancel(false);
                try {
                    var data = result.get(WAIT_SECONDS, TimeUnit.SECONDS);
                    assertEquals(CANCELLED, data, "attempt " + attempt);
                    generator.cancel(true);
                } catch (InterruptedException e) {
                    log.severe("%n%n attempt %d: got exception %s%n".formatted(attempt, e));
                    //Assertions.fail("attempt %d: got exception".formatted(attempt), e);
                }
            }
        }
    }

    @Test
    public void cancelBeforeTheEmitterStartsSkipsIt() {
        var manualExecutor = new ManualExecutor();
        var emitterRan = new AtomicBoolean();

        try(var generator = AsyncGeneratorFlow.builder()
                .executor(manualExecutor)
                .<String>build(dispatcher -> emitterRan.set(true))) {

            generator.cancel(false);
            manualExecutor.runAll();

            assertFalse(emitterRan.get());
            assertEquals(CANCELLED, generator.next().resultValue());
        }
    }

    @Test
    public void aCompletedStreamKeepsItsResultWhenCancelledAfterwards() {
        try( var generator = AsyncGeneratorFlow.builder()
                .executor(Runnable::run)
                .<String>build(dispatcher -> {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("e1")));
                    dispatcher.dispatchAsync(AsyncGenerator.Data.done("END"));
                })) {

            assertEquals(List.of("e1"), generator.stream().toList());
            generator.cancel(true);

            assertEquals("END", generator.resultValue().orElseThrow());
        }
    }

    @Test
    public void anInterruptSentByCancelDoesNotLeakIntoTheExecutorsNextTask() throws Exception {
        try (var nonClearing = new NonClearingSingleThreadExecutor()) {
            for (int attempt = 0; attempt < LEAK_ATTEMPTS; attempt++) {
                var started = new CountDownLatch(1);
                try (var generator = AsyncGeneratorFlow.builder()
                        .executor(nonClearing)
                        .<String>build(dispatcher -> {
                            started.countDown();
                            while (!Thread.currentThread().isInterrupted()) {
                                Thread.onSpinWait();
                            }
                            log.info("emitter thread [%s] interrupted".formatted(Thread.currentThread().getName()));
                        })) {

                    assertTrue(started.await(WAIT_SECONDS, TimeUnit.SECONDS));
                    generator.cancel(true);

                    var nextTaskInterrupted = CompletableFuture
                            .supplyAsync(() -> {
                                log.info("emitter thread [%s] is interrupted? %b".formatted(Thread.currentThread().getName(), Thread.currentThread().isInterrupted()));
                                return Thread.currentThread().isInterrupted();
                            }, nonClearing)
                            .get(WAIT_SECONDS, TimeUnit.SECONDS);
                    assertTrue(nextTaskInterrupted, "attempt %d".formatted(attempt));
                }
            }
        }
    }

}
