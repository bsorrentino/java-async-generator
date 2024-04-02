package org.bsc.async;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.ForkJoinPool.commonPool;
import static org.junit.jupiter.api.Assertions.*;

public class AsyncGeneratorQueueTest {

    private AsyncGenerator<String> generateTestData() {

        final AsyncGeneratorQueue<String> asyncGenerator =
                AsyncGeneratorQueue.<String>builder()
                        .build();

        commonPool().execute( () -> {
            try {
                for( int i = 0 ; i < 10 ; ++i ) {
                    asyncGenerator.put(completedFuture("e"+i) );
                }
            } catch (Exception e) {
                asyncGenerator.closeExceptionally(e);
            }
            finally {
                asyncGenerator.close();
            }

        });

        return asyncGenerator;
    }
    private AsyncGenerator<String> generateTestDataWithException( Throwable ex ) {

        final AsyncGeneratorQueue<String> asyncGenerator =
                AsyncGeneratorQueue.<String>builder()
                        .build();

        commonPool().execute( () -> {
            try {
                for( int i = 0 ; i < 10 ; ++i ) {
                    asyncGenerator.put(completedFuture("e"+i) );
                }
                asyncGenerator.closeExceptionally(ex);

            } catch (Exception e) {
                asyncGenerator.closeExceptionally(e);
            }
            finally {
                asyncGenerator.close();
            }

        });

        return asyncGenerator;
    }

    @Test
    //@Disabled
    public void asyncQueueDirectTest() throws Exception {

        // AsyncQueue initialized with a direct executor. No thread is used on next() invocation

        AsyncGenerator<String> generator = generateTestData();

        List<String> result = new ArrayList<>();

        for (var i : generator) {
            result.add(i);
        }

        System.out.println("Finished");

        generator.forEachAsync( result::add ).thenAccept( t -> {
            System.out.println( "Finished");

        });

        assertEquals( result.size(), 10 );
        assertIterableEquals(result, List.of("e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"));

    }

    @Test
    //@Disabled
    public void asyncQueueToStreamTest() throws Exception {

        // AsyncQueue initialized with a direct executor. No thread is used on next() invocation
        final AsyncGenerator<String> generator = generateTestData();

        var result = generator.stream();

        var lastElement =   result.reduce((a, b) -> b);

        assertTrue( lastElement.isPresent());
        assertEquals( lastElement.get(), "e9" );

    }

    @Test
    //@Disabled
    public void asyncQueueIteratorExceptionTest() throws Exception {

        // AsyncQueue initialized with a direct executor. No thread is used on next() invocation
        final AsyncGenerator<String> generator = generateTestDataWithException(new Exception("test"));

        var result = generator.stream();

        assertThrows( Exception.class,  () -> result.reduce((a, b) -> b ));

    }

    @Test
    //@Disabled
    public void asyncQueueForEachExceptionTest() throws Exception {

        // AsyncQueue initialized with a direct executor. No thread is used on next() invocation
        final AsyncGenerator<String> generator = generateTestDataWithException(new Exception("test"));

        assertThrows( Exception.class, () -> generator.forEachAsync( System.out::println ).get() );

    }

    @Test
    @Disabled
    public void exceptionTest() throws Exception {
        try {
            throw new Exception("test");
        }
        catch (Exception e) {
            System.out.println( "Exception: " + e.getMessage());
        }
        finally {
            System.out.println( "Finally: ");
        }
    }

    class testAutocloseable implements AutoCloseable {

        @Override
        public void close() throws Exception {
            System.out.println("AutoCloseable: ");
        }
    }
    @Test
    @Disabled
    public void exceptionAutocloseableTest() throws Exception {
        try( testAutocloseable t = new testAutocloseable() ) {
            throw new Exception("test");
        }
        catch (Exception e) {
            System.out.println( "Exception: " + e.getMessage());
        }
        finally {
            System.out.println( "Finally: ");
        }
    }

}
