package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class AsyncGeneratorQueueTest {

    @Test
    public void asyncGeneratorForEachTest() throws Exception {

        final BlockingQueue<AsyncGenerator.Data<String>> queue = new LinkedBlockingQueue<>();

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        final AsyncGenerator<String> it = AsyncGeneratorQueue.of( queue, q -> {
            for( String value: data ) {
                queue.add(AsyncGenerator.Data.of(completedFuture(value)));
            }
        });

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        List<String> iterationResult = new ArrayList<>();
        for (var i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration");

        assertEquals( data.length, forEachResult.size() );
        assertIterableEquals( List.of(data), forEachResult );
        assertEquals( 0, iterationResult.size() );
    }
    @Test
    public void asyncGeneratorIteratorTest() throws Exception {

        final BlockingQueue<AsyncGenerator.Data<String>> queue = new LinkedBlockingQueue<>();

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        final AsyncGenerator<String> it = AsyncGeneratorQueue.of( queue, q -> {
            for( String value: data ) {
                queue.add(AsyncGenerator.Data.of(completedFuture(value)));
            }
        });

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        assertEquals(  data.length, iterationResult.size() );
        assertIterableEquals( List.of(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }
    @Test
    public void asyncGeneratorStreamTest() throws Exception {

        final BlockingQueue<AsyncGenerator.Data<String>> queue = new LinkedBlockingQueue<>();

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        final AsyncGenerator<String> it = AsyncGeneratorQueue.of( queue, q -> {
            for( String value: data ) {
                queue.add(AsyncGenerator.Data.of(completedFuture(value)));
            }
        });
        List<String> iterationResult = it.stream().collect(Collectors.toList());
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        assertEquals(  data.length, iterationResult.size() );
        assertIterableEquals( List.of(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }
}

