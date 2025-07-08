package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncGeneratorTest {

    @Test
    public void asyncGeneratorForEachTest() throws Exception {
        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it =
                AsyncGenerator.map(asList(data), CompletableFuture::completedFuture);

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
        assertIterableEquals( asList(data), forEachResult );
        assertEquals( 0, iterationResult.size() );
    }

    @Test
    public void asyncGeneratorFilterTest() throws Exception {
        final String[] data = { "a1", "b2", "c3", "d4", "e1"};
        final AsyncGenerator<String> it =
                AsyncGenerator.map(asList(data), CompletableFuture::completedFuture);

        List<String> forEachResult = it.filter( s -> s.endsWith("1") )
                .collectAsync( new ArrayList<>(), (result, v) -> {
                    System.out.println( "add element: " + v);
                    result.add(v);
                } ).join();

        System.out.println( "Finished iteration");

        assertEquals( 2, forEachResult.size() );
        assertIterableEquals( asList( "a1", "e1"), forEachResult );
    }

    @Test
    public void asyncGeneratorMapTest() throws Exception {
        final String[] data = { "a1", "b2", "c3", "d4", "e1"};
        final AsyncGenerator<String> it =
                AsyncGenerator.map(asList(data), CompletableFuture::completedFuture);

        List<String> forEachResult = it.map( s -> s + "0" )
                .collectAsync( new ArrayList<>(), (result, v) -> {
                    System.out.println( "add element: " + v);
                    result.add(v);
                } ).join();

        System.out.println( "Finished iteration");

        assertEquals( data.length, forEachResult.size() );
        assertIterableEquals( asList( "a10", "b20", "c30", "d40", "e10" ), forEachResult );
    }

    @Test
    public void asyncGeneratorCollectTest() throws Exception {
        final List<Integer> data = asList( 1, 2, 3, 4, 5 );
        final AsyncGenerator<String> it = AsyncGenerator.collect(data.iterator(), ( index, add ) ->
                add.accept( Task.of( index, 500 ) )
        );

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach " + t);
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
        final AsyncGenerator<String> it = AsyncGenerator.map(asList(data).iterator(),
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
        assertIterableEquals( asList(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    @Test
    public void asyncGeneratorStreamTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};
        final AsyncGenerator<String> it = AsyncGenerator.map(asList(data).iterator(),
                CompletableFuture::completedFuture);

        List<String> iterationResult = it.stream().collect(Collectors.toList());
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        assertEquals(  data.length, iterationResult.size() );
        assertIterableEquals( asList(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    static class NestedAsyncGenerator implements AsyncGenerator<String> {
        int index = -1;
        final List<String> data = asList( "e1", "e2", "e3", null, "e4", "e5", "e6", "e7");
        final List<String> nestedData = asList( "n1", "n2", "n3", "n4", "n5");

        @Override
        public Data<String> next() {
            ++index;
            if( index >= data.size() ) {
                index = -1;
                return Data.done( data.size()-1 );
            }
            if( index == 3) {
                return Data.composeWith(
                        AsyncGenerator.map(nestedData.iterator(), CompletableFuture::completedFuture),
                        (v) -> {
                            System.out.println( "Nested done ");
                            assertNull(v);
                        } );
            }

            return Data.of( data.get( index ) );
        }
    }




    @Test
    public void asyncEmbedGeneratorTest() throws Exception {
        final List<String> expected = asList( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7");
        AsyncGenerator.WithEmbed<String> it = new  AsyncGenerator.WithEmbed<>(new NestedAsyncGenerator());

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

    @Test
    public void asyncEmbedGeneratorWithResultTest() throws Exception {
        final List<String> expected = asList( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7");
        AsyncGenerator.WithEmbed<String> it = new  AsyncGenerator.WithEmbed<>(new NestedAsyncGenerator(), result -> {
            System.out.println( "generator done " );
            assertNotNull( result );
            assertEquals( 7, result );

        });

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add )
                .thenAccept( result -> {
                    assertEquals( 7, result );
                    System.out.println( "Finished forEach" );
                })
                .join();

        assertEquals( 12, forEachResult.size() );
        assertIterableEquals( expected, forEachResult );
        assertEquals( 2, it.resultValues().size() );
        Object resultValue = it.resultValues().getFirst().resultValue;
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue );

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
        }
        System.out.println( "Finished Iterator");
        assertEquals( 2, it.resultValues().size() );
        resultValue = it.resultValues().getFirst().resultValue;
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue );


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
        assertEquals( 2, it.resultValues().size() );
        resultValue = it.resultValues().getFirst().resultValue;
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue );

    }

    static class AsyncGeneratorWithResult implements AsyncGenerator<String> {
        final List<String> elements;
        int index = -1;

        AsyncGeneratorWithResult( List<String> elements ) {
            this.elements = elements;
        }

        @Override
        public Data<String> next() {
            ++index;
            if( index >= elements.size() ) {
                index = -1;
                return Data.done( elements.size() );
            }
            return Data.of( elements.get( index ) );
        }

    }
    @Test
    public void asyncGeneratorWithResultTest() throws Exception {
        var generator = new AsyncGeneratorWithResult(
                List.of( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7"));

        AsyncGenerator<String> it = new AsyncGenerator.WithResult<>(generator);

        it.stream().forEach( System.out::print );
        System.out.println();

        assertTrue( AsyncGenerator.resultValue(it).isPresent() );
        assertEquals( 12, AsyncGenerator.resultValue(it).get() );

        for( var element : it ) {
            System.out.print( element );
        }

        assertTrue( AsyncGenerator.resultValue(it).isPresent() );
        assertEquals( 12, AsyncGenerator.resultValue(it).get() );
    }
}
