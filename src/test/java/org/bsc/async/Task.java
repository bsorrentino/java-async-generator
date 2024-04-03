package org.bsc.async;

import java.util.concurrent.CompletableFuture;

public class Task {

    static CompletableFuture<String> of(int index, long delayInMills) {
        return CompletableFuture.supplyAsync( () -> {
            try {
                Thread.sleep( delayInMills );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "e" + index;
        } );
        //return completedFuture("e" + index);
    }
    static CompletableFuture<String> of(int index) {
        return CompletableFuture.supplyAsync( () -> {
            return "e" + index;
        } );
    }
}
