package org.bsc.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@Disabled
public class AsyncGeneratorTest {

    @Test
    public void asyncGeneratorForEachTest() throws Exception {
        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it = AsyncGenerator.map(Arrays.asList(data).iterator(),
                CompletableFuture::completedFuture);

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
    public void asyncGeneratorCollectTest() throws Exception {
        final List<Integer> data = List.of( 1, 2, 3, 4, 5 );
        final AsyncGenerator<String> it = AsyncGenerator.collect(data.iterator(), ( index, add ) ->
                add.accept( Task.of( index, 500 ) )
        );

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


        assertEquals( data.size(), forEachResult.size() );
        List<String> expected = data.stream().map( index -> "e"+index).collect(Collectors.toList());
        assertIterableEquals( expected, forEachResult );
        assertEquals( 0, iterationResult.size() );
    }
    @Test
    public void asyncGeneratorIteratorTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it = AsyncGenerator.map(Arrays.asList(data).iterator(),
                CompletableFuture::completedFuture);

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

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it = AsyncGenerator.map(Arrays.asList(data).iterator(),
                CompletableFuture::completedFuture);

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
