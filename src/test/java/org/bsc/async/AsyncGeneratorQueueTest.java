package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.bsc.async.AsyncGenerator.Cancellable.CANCELLED;
import static org.junit.jupiter.api.Assertions.*;

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
        assertIterableEquals( Arrays.asList(data), iterationResult );
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
        assertIterableEquals( Arrays.asList(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    @Test
    public void asyncGeneratorWithResultStreamTest() throws Exception {

        final BlockingQueue<AsyncGenerator.Data<String>> queue = new LinkedBlockingQueue<>();

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        final AsyncGenerator.WithResult<String> it = new AsyncGenerator.WithResult<>( new AsyncGeneratorQueue.Generator<>(queue) );

        commonPool().execute( () -> {
            try {
                for( String value: data ) {
                    queue.add(AsyncGenerator.Data.of(completedFuture(value)));
                }
            }
            catch( Throwable ex ) {
                CompletableFuture<String> error = new CompletableFuture<>();
                error.completeExceptionally(ex);
                queue.add( AsyncGenerator.Data.of(error));
            }
            finally {
                queue.add(AsyncGenerator.Data.done( "END"));
            }

        });

        List<String> iterationResult = it.stream().collect(Collectors.toList());
        System.out.println( "Finished iteration " + iterationResult);

        List<String> forEachResult = new ArrayList<>();
        it.forEachAsync( forEachResult::add ).thenAccept( t -> {
            System.out.println( "Finished forEach");
        }).join();


        assertTrue( it.resultValue().isPresent() );
        assertEquals( "END", it.resultValue().get() );
        assertEquals(  data.length, iterationResult.size() );
        assertIterableEquals( Arrays.asList(data), iterationResult );
        assertEquals( 0, forEachResult.size() );
    }

    @Test
    public void asyncGeneratorCancelTest() throws Exception {

        final BlockingQueue<AsyncGenerator.Data<String>> queue = new LinkedBlockingQueue<>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );

        final AsyncGenerator.WithResult<String> it = new AsyncGenerator.WithResult<>( new AsyncGeneratorQueue.Generator<>(queue) );

        var executor = Executors.newFixedThreadPool(10);

        executor.execute( () -> {
            try {
                for( String value: data ) {
                    Thread.sleep( 1000 );
                    queue.add(AsyncGenerator.Data.of(completedFuture(value)));
                }
                queue.add(AsyncGenerator.Data.done( "END"));
            }
            catch( Throwable ex ) {
                CompletableFuture<String> error = new CompletableFuture<>();
                error.completeExceptionally(ex);
                queue.add( AsyncGenerator.Data.error(ex));
            }

        });

        var forEachResult = new ArrayList<String>();

        executor.execute( () -> {
            try {
                Thread.sleep(3000);
                it.cancel( true );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var futureResult = it.forEachAsync( value -> {
                    System.out.println( value );
                    forEachResult.add(value);
                });

        var result = futureResult.get( 10, TimeUnit.SECONDS);

        assertNotNull( result );
        assertEquals( CANCELLED, result );
        assertTrue( forEachResult.size() < data.size() );


    }

}

