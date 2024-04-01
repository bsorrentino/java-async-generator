package org.bsc.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.async.AsyncFunction.consumer_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@Disabled
public class AsyncGeneratorTest {

    class SimpleAsyncGenerator implements AsyncGenerator<String> {
        private int cursor = 0;
        final String[] data = { "e1", "e2", "e3", "e4", "e5"};


        @Override
        public CompletableFuture<AsyncGenerator.Data<String>> next() {

            if (cursor == data.length) {
                return completedFuture(new Data<>(null, true) );
            }

            return completedFuture(new Data<>(data[cursor++], false));
        }

    }
    @Test
    public void asyncGeneratorForEachTest() throws Exception {

        final SimpleAsyncGenerator it = new SimpleAsyncGenerator();

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( consumer_async(forEachResult::add)).thenAccept( t -> {
            System.out.println( "Finished forEach");
        });

        List<String> iterationResult = new ArrayList<>();
        for (var i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration");

        assertEquals( it.data.length, forEachResult.size() );
        assertIterableEquals( List.of(it.data), forEachResult );
        assertEquals( 0, iterationResult.size() );
    }
    @Test
    public void asyncGeneratorIteratorTest() throws Exception {

        final SimpleAsyncGenerator it = new SimpleAsyncGenerator();

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( consumer_async(forEachResult::add)).thenAccept( t -> {
            System.out.println( "Finished forEach");
        });

        assertEquals(  it.data.length, iterationResult.size() );
        assertIterableEquals( List.of(it.data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }
    @Test
    public void asyncGeneratorStreamTest() throws Exception {

        final SimpleAsyncGenerator it = new SimpleAsyncGenerator();

        List<String> iterationResult = it.stream().collect(Collectors.toList());
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( consumer_async(forEachResult::add)).thenAccept( t -> {
            System.out.println( "Finished forEach");
        });

        assertEquals(  it.data.length, iterationResult.size() );
        assertIterableEquals( List.of(it.data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }
}
