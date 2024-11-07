package org.bsc.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
        for (String i : it) {
            iterationResult.add(i);
            System.out.println(i);
        }
        System.out.println( "Finished iteration");

        assertEquals( data.length, forEachResult.size() );
        assertIterableEquals( Arrays.asList(data), forEachResult );
        assertEquals( 0, iterationResult.size() );
    }
    @Test
    public void asyncGeneratorCollectTest() throws Exception {
        final List<Integer> data = Arrays.asList( 1, 2, 3, 4, 5 );
        final AsyncGenerator<String> it = AsyncGenerator.collect(data.iterator(), ( index, add ) ->
                add.accept( Task.of( index, 500 ) )
        );

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
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
        assertIterableEquals( Arrays.asList(data), iterationResult );
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
        assertIterableEquals( Arrays.asList(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    static class NestedAsyncGenerator implements AsyncGenerator<String> {
        int index = -1;
        final List<String> data = Arrays.asList( "e1", "e2", "e3", null, "e4", "e5", "e6", "e7");
        final List<String> nestedData = Arrays.asList( "n1", "n2", "n3", "n4", "n5");

        @Override
        public Data<String> next() {
            ++index;
            if( index >= data.size() ) {
                index = -1;
                return Data.done( data.size()-1 );
            }
            if( index == 3) {
                return Data.of( AsyncGenerator.map(nestedData.iterator(), CompletableFuture::completedFuture) );
            }

            return Data.of( data.get( index ) );
        }
    }
    @Test
    public void asyncNestedGeneratorTest() throws Exception {
        final List<String> expected = Arrays.asList( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7");
        final NestedAsyncGenerator it = new NestedAsyncGenerator();

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add )
                .thenAccept( result -> {
                    assertEquals( 7, result );
                    System.out.println( "Finished forEach" );
                })
                .join();

        assertEquals( 12, forEachResult.size() );
        assertIterableEquals( expected, forEachResult );

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
        }

        System.out.println( "Finished Iterator");
        assertEquals( 12, iterationResult.size() );
        assertIterableEquals( expected, iterationResult );

        forEachResult.clear();
        it.forEachAsync( forEachResult::add )
                .thenAccept( result -> {
                    assertEquals( 7, result );
                    System.out.println( "Finished forEach" );
                })
                .join();

        assertEquals( 12, forEachResult.size() );
        assertIterableEquals( expected, forEachResult );

    }
}
