package org.bsc.async;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class Task {

    static CompletableFuture<String> async(int index, Duration delay ) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Thread.sleep( delay.toMillis() );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "e%d".formatted(index);
        } );
    }
    static CompletableFuture<String> async(int index) {
        return CompletableFuture.supplyAsync( () -> "e%d".formatted(index));
    }

    static CompletableFuture<String> sync(int index) {
        System.out.printf("task[%d] - thread[%s]%n", index, Thread.currentThread().getName());
        return completedFuture( "e%d".formatted(index));
    }
}
