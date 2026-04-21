package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FutureCancellationTest {

    @Test
    public void cancelFutureTest() throws Exception {
        var executedSteps = new AtomicInteger(0);
        var exec = Executors.newSingleThreadExecutor();

        var future = exec.submit(() -> {
            try {
                for( var i = 0 ; i < 1000; ++i ) {
                    System.out.printf("%d ) Start Working...\n", executedSteps.get());
                    Thread.sleep(200); // throws InterruptedException
                    System.out.printf("%d ) End Working...\n", executedSteps.getAndIncrement());
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted!");
                Thread.currentThread().interrupt(); // restore flag
            }
        });

        Thread.sleep(1000);
        future.cancel(true); // sends interrupt
        exec.shutdown();

        assertTrue( future.isCancelled() );
        assertTrue( future.isDone() );
        assertEquals( 4, executedSteps.get() );

    }

    @Test
    public void cancelCompletableFutureTest() throws Exception {
        var executedSteps = new AtomicInteger(0);
        var exec = Executors.newSingleThreadExecutor();

        var future = CompletableFuture.runAsync(() -> {
            try {
                for( var i = 0 ; i < 1000; ++i ) {
                    System.out.printf("%d ) Start Working...\n", executedSteps.get());
                    Thread.sleep(200); // throws InterruptedException
                    System.out.printf("%d ) End Working...\n", executedSteps.getAndIncrement());
                }
            } catch (Exception e) {
                System.out.println("Interrupted!");
                Thread.currentThread().interrupt(); // restore flag
            }
        }, exec);

        Thread.sleep(1000);
        future.cancel(true); // sends interrupt

        assertTrue( future.isCancelled() );
        assertTrue( future.isDone() );
        assertEquals( 4, executedSteps.get() );

        exec.shutdown();

    }

    @Test
    public void cancelCompletableFutureChainTest() throws Exception {
        var executedSteps = new AtomicInteger(0);
        var exec = Executors.newSingleThreadExecutor();

        final var future1 = CompletableFuture.runAsync(() -> {
            try {
                for( var i = 0 ; i < 1000; ++i ) {
                    System.out.printf("%d ) Start Working...\n", executedSteps.get());
                    Thread.sleep(200); // throws InterruptedException
                    System.out.printf("%d ) End Working...\n", executedSteps.getAndIncrement());
                }
            } catch (Exception e) {
                System.out.println("Interrupted!");
                Thread.currentThread().interrupt(); // restore flag
            }
        }, exec);

        final var future2 = CompletableFuture.runAsync( () -> {
            try {
                Thread.sleep(1000);
                future1.cancel(true); // sends interrupt
                System.out.println( "Future cancelled");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        final var ex = assertThrowsExactly( CompletionException.class, () -> CompletableFuture.allOf(future1, future2).join());
        assertInstanceOf(CancellationException.class, ex.getCause());

        assertTrue( future1.isCancelled() );
        assertTrue( future1.isDone() );
        assertEquals( 4, executedSteps.get() );

        exec.shutdown();

    }

}
