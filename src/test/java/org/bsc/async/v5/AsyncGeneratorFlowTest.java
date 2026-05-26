package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.bsc.async.AsyncGenerator.Cancellable.CANCELLED;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncGeneratorFlowTest {

    @Test
    public void asyncGeneratorForEachTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};


        try( final var it = AsyncGeneratorFlow.<String>create(dispatcher -> {
            for( String value: data ) {
                dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture(value)));
            }
            dispatcher.dispatchAsync(AsyncGenerator.Data.done("END") );
        })) {

            List<String> forEachResult = new ArrayList<>();
            it.forEachAsync(forEachResult::add).thenAccept(t -> {
                System.out.println("Finished forEach");
            }).join();

            List<String> iterationResult = new ArrayList<>();
            for (String i : it) {
                iterationResult.add(i);
                System.out.println(i);
            }
            System.out.println("Finished iteration");

            assertEquals(data.length, forEachResult.size());
            assertIterableEquals(Arrays.asList(data), forEachResult);
            assertEquals(0, iterationResult.size());
        }
    }
    @Test
    public void asyncGeneratorIteratorTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        try( final var it = AsyncGeneratorFlow.<String>create( dispatcher -> {
            for( String value: data ) {
                dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture(value)));
            }
            dispatcher.dispatchAsync(AsyncGenerator.Data.done("END") );
        })) {

            List<String> iterationResult = new ArrayList<>();
            for (String i : it) {
                iterationResult.add(i);
                System.out.println(i);
            }
            System.out.println("Finished iteration " + iterationResult);

            List<String> forEachResult = new ArrayList<>();
            it.forEachAsync(forEachResult::add).thenAccept(t -> {
                System.out.println("Finished forEach");
            }).join();

            assertEquals(data.length, iterationResult.size());
            assertIterableEquals(Arrays.asList(data), iterationResult);
            assertEquals(0, forEachResult.size());
        }
    }
    @Test
    public void asyncGeneratorStreamTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        try( final var it = AsyncGeneratorFlow.<String>create( dispatcher -> {
            for( String value: data ) {
                dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture(value)));
            }
            dispatcher.dispatchAsync(AsyncGenerator.Data.done("END") );
        })) {
            List<String> iterationResult = it.stream().collect(Collectors.toList());
            System.out.println("Finished iteration " + iterationResult);

            List<String> forEachResult = new ArrayList<>();
            it.forEachAsync(forEachResult::add).thenAccept(t -> {
                System.out.println("Finished forEach");
            }).join();

            assertEquals(data.length, iterationResult.size());
            assertIterableEquals(Arrays.asList(data), iterationResult);
            assertEquals(0, forEachResult.size());
        }
    }

    @Test
    public void asyncGeneratorWithResultStreamTest() throws Exception {

        final String[] data = { "e1", "e2", "e3", "e4", "e5"};

        try( final var it = AsyncGeneratorFlow.builder()
                                .executor(commonPool())
                                .processor( new BlockingQueueProcessor<>())
                                .<String>build( dispatcher -> {
            try {
                for( String value: data ) {
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture(value)));
                }
            }
            catch( Throwable ex ) {
                dispatcher.dispatchAsync( AsyncGenerator.Data.error(ex));
            }
            finally {
                dispatcher.dispatchAsync(AsyncGenerator.Data.done( "END"));
            }

        })) {

            List<String> iterationResult = it.stream().collect(Collectors.toList());
            System.out.println("Finished iteration " + iterationResult);

            List<String> forEachResult = new ArrayList<>();
            it.forEachAsync(forEachResult::add).thenAccept(t -> {
                System.out.println("Finished forEach");
            }).join();


            assertTrue(it.resultValue().isPresent());
            assertEquals("END", it.resultValue().get());
            assertEquals(data.length, iterationResult.size());
            assertIterableEquals(Arrays.asList(data), iterationResult);
            assertEquals(0, forEachResult.size());
        }
    }

    @Test
    public void asyncGeneratorCancelTest() throws Exception {

        final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );

        final var executor = Executors.newFixedThreadPool(10);

        try( final var it = AsyncGeneratorFlow.builder()
                .executor(executor)
                .processor( new BlockingQueueProcessor<>())
                .<String>build( dispatcher -> {
            try {
                for( String value: data ) {
                    Thread.sleep( 1000 );
                    dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture(value)));
                }
                dispatcher.dispatchAsync(AsyncGenerator.Data.done( "END"));
            }
            catch( Throwable ex ) {
                dispatcher.dispatchAsync( AsyncGenerator.Data.error(ex));
            }

        })) {

            var forEachResult = new ArrayList<String>();

            executor.execute(() -> {
                try {
                    Thread.sleep(3000);
                    it.cancel(true);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            var futureResult = it.forEachAsync(value -> {
                System.out.println(value);
                forEachResult.add(value);
            });

            var result = futureResult.get(10, TimeUnit.SECONDS);

            assertNotNull(result);
            assertEquals(CANCELLED, result);
            assertTrue(forEachResult.size() < data.size());

        } finally {
            executor.shutdown();
        }
    }

    record Result<T>( CompletableFuture<T> publisher, Supplier<AsyncGeneratorFlow.Generator<T>> generator ) {

        public Result( CompletableFuture<T> publisher ) {
            this( publisher, null );
        }
    }

    private  Result<String> newEmbedAsyncGeneratorFlow(String prefix ) {

        final Supplier<AsyncGeneratorFlow.Generator<String>> generator = () ->
                AsyncGeneratorFlow.builder()
                        .executor(Runnable::run)
                        .build( dispatcher -> {

                    try {
                        for( int i = 0; i < 5; ++i ) {
                            //Thread.sleep(1000);
                            dispatcher.dispatchAsync(AsyncGenerator.Data.of(completedFuture("%s%d".formatted(prefix, i+1))));
                        }
                        // !!IMPORTANT!!
                        // Embed must not dispatch end marker
                        dispatcher.dispatchAsync( AsyncGenerator.Data.done( "DONE"));
                    } catch (Throwable ex) {
                        dispatcher.dispatchAsync(AsyncGenerator.Data.error(ex));
                    }});

        return new Result<>(null, generator);
    }

    @Test
    public void asyncGeneratorWithResultStreamAndEmbedTest() throws Exception {

        final var embed = newEmbedAsyncGeneratorFlow("e4." );

        List<Result<String>> data = List.of(
                new Result<>( completedFuture("e1")),
                new Result<>( completedFuture("e2")),
                new Result<>( completedFuture("e3")),
                embed,
                new Result<>( completedFuture("e5"))
        );

        try( final var it = AsyncGeneratorFlow.builder()
                                .<String>build( $1 -> {
            try {
                for (final var value : data) {

                    if( value.generator() != null ) {
                        final var generator = value.generator().get();
                        generator.stream().forEach( v -> {
                            try {
                                $1.dispatchAsync( AsyncGenerator.Data.of( completedFuture(v) ) );
                            } catch (Throwable ex) {
                                $1.dispatchAsync( AsyncGenerator.Data.error(ex));
                            }
                        });

                    }
                    else {
                        $1.dispatchAsync( AsyncGenerator.Data.of(value.publisher()));
                    }
                }
            } catch (Throwable ex) {
                $1.dispatchAsync(AsyncGenerator.Data.error(ex));
            } finally {
                $1.dispatchAsync(AsyncGenerator.Data.done("END"));
            }

        })) {

            List<String> iterationResult = it.stream().collect(Collectors.toList());
            System.out.println( "Finished iteration " + iterationResult);

            List<String> forEachResult = new ArrayList<>();
            final var result = it.forEachAsync( forEachResult::add ).whenComplete( (v,ex) -> {
                System.out.printf( "Finished forEach with value '%s'%n", v);
            }).join();

            assertNotNull(result);
            assertEquals("END", result);
            assertEquals(9, iterationResult.size());
            assertIterableEquals(
                    List.of( "e1", "e2", "e3", "e4.1", "e4.2", "e4.3", "e4.4", "e4.5", "e5" ),
                    iterationResult);
            assertEquals(0, forEachResult.size());
        }
    }

}

