package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class AsyncGeneratorAsyncTest {

    @Test
    public void asyncGeneratorForEachTest() throws Exception {
        var executor = new ForkJoinPool(10);

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it =
                AsyncGenerator.map(asList(data), CompletableFuture::completedFuture);

        List<String> forEachResult = new ArrayList<>();
        it.async( executor )
                .forEachAsync( forEachResult::add )
                .thenAccept(t -> {
                    System.out.println( "Finished forEach");
                }).join();

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration");

        assertEquals( data.length, forEachResult.size() );
        assertIterableEquals( asList(data), forEachResult );
        assertEquals( 0, iterationResult.size() );
    }

}
