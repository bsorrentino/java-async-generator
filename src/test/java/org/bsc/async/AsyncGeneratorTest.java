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
        final List<String> data = List.of( "e1", "e2", "e3", "e4", "e5" );
        final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator());

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
        assertIterableEquals( data, forEachResult );
        assertEquals( 0, iterationResult.size() );
    }

    @Test
    public void asyncGeneratorForEachCancelTest() throws Exception {
/*
        final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );
        final AsyncGenerator<String> it =
                AsyncGenerator.map(data, CompletableFuture::completedFuture);
        final var cancellableIt = new AsyncGenerator.Cancellable<>(it);

        List<String> forEachResult = new ArrayList<>();
        var future = cancellableIt.async(Executors.newSingleThreadExecutor())
            .forEachAsync( value -> {

                try {
                    System.out.printf( "adding element: %s\n", value);
                    Thread.sleep( 1000 );
                    forEachResult.add(value);
                    System.out.printf( "added element: %s\n", value);
                } catch (InterruptedException e) {
                    System.err.printf("interrupted on : %s\n", value );
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e);
                }
            } );

        Thread.sleep( 4000 );
        cancellableIt.cancel();

        //var result = future.get( 5, TimeUnit.SECONDS);

        //assertNotNull( result );
        //assertEquals( CANCELLED, result );
        assertEquals( 3, forEachResult.size() );
        assertIterableEquals( data.subList(0,3), forEachResult );
*/
    }


    @Test
    public void asyncGeneratorMapTest() throws Exception {
        final List<String> data = List.of( "a1", "b2", "c3", "d4", "e1" );
        final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator());

        var forEachResult = it.map( s -> s + "0" )
                        .reduceAsync( new ArrayList<>(), (result, v) -> {
                            System.out.println( "add element: " + v);
                            result.add(v);
                            return result;
                        } ).join();

        System.out.println( "Finished iteration");

        assertEquals( data.size(), forEachResult.size() );
        assertIterableEquals( asList( "a10", "b20", "c30", "d40", "e10" ), forEachResult );
    }

    @Test
    public void asyncGeneratorFlatMapTest() throws Exception {
        final var data = List.of( 1, 2, 3, 4, 5 );

        final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator())
                .flatMap( index -> Task.of( index, 500 )  );

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

        final var data = List.of( "e1", "e2", "e3", "e4", "e5");
        final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator());

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

        assertEquals(  data.size(), iterationResult.size() );
        assertIterableEquals( data, iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    @Test
    public void asyncGeneratorStreamTest() throws Exception {

        final var data = List.of( "e1", "e2", "e3", "e4", "e5");
        final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator());
        List<String> iterationResult = it.stream().collect(Collectors.toList());
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();

        assertEquals(  data.size(), iterationResult.size() );
        assertIterableEquals(data, iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    static class NestedAsyncGenerator extends AsyncGenerator.Base<String> {
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
                        AsyncGenerator.from(nestedData.iterator()),
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
        final List<String> expected = List.of( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7");
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
        Object resultValue = it.resultValues().getFirst().resultValue();
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue() );

        List<String> iterationResult = new ArrayList<>();
        for (String i : it) {
            iterationResult.add(i);
        }
        System.out.println( "Finished Iterator");
        assertEquals( 2, it.resultValues().size() );
        resultValue = it.resultValues().getFirst().resultValue();
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue() );


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
        resultValue = it.resultValues().getFirst().resultValue();
        assertNotNull( resultValue );
        assertEquals( 7, resultValue );
        assertNull( it.resultValues().getLast().resultValue());

    }

    @Test
    public void asyncEmbedGeneratorWithResultCancelTest() throws Exception {
        final List<String> expected = List.of( "e1", "e2", "e3", "n1", "n2", "n3", "n4", "n5", "e4", "e5", "e6", "e7");
        AsyncGenerator.WithEmbed<String> it = new  AsyncGenerator.WithEmbed<>(new NestedAsyncGenerator(), result -> {
            System.out.println( "generator done " );
            assertNotNull( result );
            assertEquals( 7, result );

        });

        CompletableFuture.runAsync( () -> {
            try {
                Thread.sleep(2000);
                var cancelled = it.cancel( true );
                assertTrue( cancelled );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( value  -> {
                    try {
                        Thread.sleep(200);
                        forEachResult.add( value );

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } )
                .thenAccept( result -> {
                    assertEquals( 7, result );
                    System.out.println( "Finished forEach" );
                })
                ;

        assertTrue( it.isCancelled() , "generator should be cancelled");
        assertTrue( forEachResult.size() < 12, "result should be partial" );
        assertEquals( 2, it.resultValues().size() );

    }

    static class AsyncGeneratorWithResult extends AsyncGenerator.Base<String> {
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
