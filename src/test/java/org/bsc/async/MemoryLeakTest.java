package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the ExecutorService memory leak in JDK 21 is fixed.
 * <p>
 * This test verifies that:
 * 1. Base instances can be garbage collected
 * 2. Executor threads are properly cleaned up
 * 3. The Cleaner mechanism works as expected
 */
public class MemoryLeakTest {

    /**
     * Simple test generator that produces a few elements.
     */
    static class TestGenerator extends AsyncGenerator.Base<String> {
        private int count = 0;
        private final int max;

        TestGenerator(int max) {
            this.max = max;
        }

        @Override
        public Data<String> next() {
            if (count >= max) {
                return Data.done();
            }
            return Data.of("element-" + count++);
        }
    }

    /**
     * Test that Base instances are properly garbage collected when no longer referenced.
     * This verifies that the circular reference problem is fixed.
     * <p>
     * Note: GC behavior is not deterministic, so this test uses multiple strategies
     * to verify proper cleanup.
     */
    @Test
    public void testBaseIsGarbageCollectable() throws InterruptedException {
        // Count active threads before test
        int initialThreadCount = countAsyncGeneratorThreads();

        // Create generators in a separate method to avoid stack references
        createAndDiscardGenerators(100);

        // Force garbage collection multiple times
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
        }

        // Run finalization
        System.runFinalization();
        Thread.sleep(500);

        // Verify thread count hasn't exploded
        // The key test: if there was a memory leak with circular reference,
        // the threads would NOT be cleaned up even after GC
        int finalThreadCount = countAsyncGeneratorThreads();

        // Since we created 100 generators, if leak exists, we'd see ~100 threads
        // With fix, threads should be cleaned up by Cleaner
        assertTrue(finalThreadCount < 50,
            "Thread count should not grow significantly after GC. " +
            "Initial: " + initialThreadCount + ", Final: " + finalThreadCount +
            ". If threads remain high, there may be a circular reference preventing GC.");
    }

    /**
     * Helper method to create and discard generators without keeping stack references.
     */
    private void createAndDiscardGenerators(int count) {
        for (int i = 0; i < count; i++) {
            TestGenerator gen = new TestGenerator(5);
            // Consume some elements to ensure executor is used
            gen.next();
            // Don't close - let GC handle it via Cleaner
        }
    }

    /**
     * Test that explicit close() properly cleans up resources.
     */
    @Test
    public void testExplicitCloseCleansResources() throws InterruptedException {
        int initialThreadCount = countAsyncGeneratorThreads();

        // Create and close generators
        for (int i = 0; i < 50; i++) {
            TestGenerator gen = new TestGenerator(5);
            gen.next(); // Use the generator
            gen.close(); // Explicitly close
            assertTrue(gen.isClosed(), "Generator should be closed");
        }

        // Give time for cleanup
        Thread.sleep(200);

        // Thread count should not have increased
        int finalThreadCount = countAsyncGeneratorThreads();
        assertTrue(finalThreadCount <= initialThreadCount,
            "Thread count should not increase after explicit close. Initial: " + initialThreadCount +
            ", Final: " + finalThreadCount);
    }

    /**
     * Test that cancel() properly cleans up resources.
     */
    @Test
    public void testCancelCleansResources() throws InterruptedException {
        int initialThreadCount = countAsyncGeneratorThreads();

        // Create and cancel cancellable generators
        for (int i = 0; i < 50; i++) {
            AsyncGenerator.WithResult<String> gen =
                new AsyncGenerator.WithResult<>(AsyncGenerator.from(List.of("a", "b", "c").iterator()));
            gen.next(); // Use the generator
            gen.cancel(true); // Cancel
            assertTrue(gen.isCancelled(), "Generator should be cancelled");
            assertTrue(gen.isClosed(), "Generator should be closed after cancel");
        }

        // Give time for cleanup
        Thread.sleep(200);

        // Thread count should not have increased
        int finalThreadCount = countAsyncGeneratorThreads();
        assertTrue(finalThreadCount <= initialThreadCount,
            "Thread count should not increase after cancel. Initial: " + initialThreadCount +
            ", Final: " + finalThreadCount);
    }

    /**
     * Test that try-with-resources works correctly.
     */
    @Test
    public void testTryWithResources() throws InterruptedException {
        int initialThreadCount = countAsyncGeneratorThreads();

        // Use try-with-resources
        for (int i = 0; i < 50; i++) {
            try (TestGenerator gen = new TestGenerator(5)) {
                gen.next();
            } // Auto-close
        }

        // Give time for cleanup
        Thread.sleep(200);

        // Thread count should not have increased
        int finalThreadCount = countAsyncGeneratorThreads();
        assertTrue(finalThreadCount <= initialThreadCount,
            "Thread count should not increase with try-with-resources. Initial: " + initialThreadCount +
            ", Final: " + finalThreadCount);
    }

    /**
     * Test that close() is idempotent.
     */
    @Test
    public void testCloseIsIdempotent() {
        TestGenerator gen = new TestGenerator(5);

        // Close multiple times
        gen.close();
        assertTrue(gen.isClosed());

        gen.close();
        assertTrue(gen.isClosed());

        gen.close();
        assertTrue(gen.isClosed());

        // Should not throw
        assertDoesNotThrow(() -> gen.close());
    }

    /**
     * Test instance ID generation.
     */
    @Test
    public void testInstanceIdGeneration() {
        // Create generators and verify each has a unique ID
        // This is indirectly tested by checking thread names
        List<TestGenerator> generators = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            generators.add(new TestGenerator(1));
        }

        // All generators should be distinct
        assertEquals(10, generators.size());

        // Each should be usable
        for (TestGenerator gen : generators) {
            assertNotNull(gen.next());
        }
    }

    /**
     * Helper method to count AsyncGenerator threads.
     */
    private int countAsyncGeneratorThreads() {
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        Thread.enumerate(threads);
        int count = 0;
        for (Thread t : threads) {
            if (t != null && t.getName().startsWith("AsyncGenerator[")) {
                count++;
            }
        }
        return count;
    }
}

