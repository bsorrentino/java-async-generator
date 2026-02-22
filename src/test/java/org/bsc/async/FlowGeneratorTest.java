package org.bsc.async;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.junit.jupiter.api.Assertions.*;

public class FlowGeneratorTest {

    @Test
    public void flowGeneratorSubscriberTest() throws Exception {

        final var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        final var publisher = new SubmissionPublisher<String>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5" );

        final var generator = FlowGenerator.fromPublisher(publisher);

        assertTrue( publisher.hasSubscribers() );

        final var submitting = runAsync( () -> {
                data.stream().peek(System.out::println).forEach( publisher::submit );
                publisher.close();
            }, executor );

        final var result = new ArrayList<>();
        final var iterating = generator.forEachAsync(result::add);

        CompletableFuture.allOf(iterating, submitting ).join();

        assertEquals( data.size(), result.size() );
        assertIterableEquals( data, result );

        System.out.printf("Core pool size: %d%n", executor.getCorePoolSize());
        System.out.printf("Largest pool size: %d%n",executor.getLargestPoolSize());
        System.out.printf("Active threads: %d%n", executor.getActiveCount());
        System.out.printf("Completed tasks: %d%n", executor.getCompletedTaskCount());

    }

    @Test
    public void flowGeneratorPublisherTest() throws Exception {

        final var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        final var queue = new LinkedBlockingQueue<AsyncGenerator.Data<String>>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5" );

        final var generator = AsyncGeneratorQueue.of( queue, q -> {

            for( String value: data ) {
                queue.add(AsyncGenerator.Data.of(completedFuture(value)));
            }
        }, executor);

        final var publisher = FlowGenerator.toPublisher( generator );

        final var result = new ArrayList<String>();

        publisher.subscribe(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                result.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println(throwable.getLocalizedMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("Completed");
            }
        });

        generator.toCompletableFuture().join();

        assertEquals( data.size(), result.size() );
        assertIterableEquals( data, result );

        System.out.println("Core pool size: " + executor.getCorePoolSize());
        System.out.println("Largest pool size: " + executor.getLargestPoolSize());
        System.out.println("Active threads: " + executor.getActiveCount());
        System.out.println("Completed tasks: " + executor.getCompletedTaskCount());

    }

    @Test
    public void flowGeneratorSubscriberAndCancelTest() throws Exception {

        final var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        final var publisher = new SubmissionPublisher<String>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );

        final var generator = FlowGenerator.fromPublisher(publisher);

        assertTrue( publisher.hasSubscribers() );

        final var submittingFuture = CompletableFuture.runAsync( () -> {

            try {
                for (String value : data) {
                    System.out.printf("publishing: %s on thread[%s]\n", value, Thread.currentThread().getName());
                    publisher.submit(value);

                    Thread.sleep(1000);
                }
            } catch( InterruptedException e ) {
                throw new CompletionException(e);
            } finally{
                publisher.close();

            }
        }, executor );

        final var cancellingFuture = CompletableFuture.runAsync( () -> {
            try {
                System.out.printf("cancelling start on thread[%s]%n",
                        Thread.currentThread().getName());

                Thread.sleep( 4000 );

                System.out.printf("generator cancelled: %s%n",
                        generator.cancel( true) );

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor );


        final var result = new ArrayList<>();
        final var iterating = generator
                        .forEachAsync( value -> {
                            try {
                                Thread.sleep(10);
                                System.out.printf("received: %s on thread[%s]%n", value, Thread.currentThread().getName());
                                result.add(value);
                            } catch (InterruptedException e) {
                                System.err.printf( "interrupted on thread[%s]%n", Thread.currentThread().getName() );
                                throw new CompletionException(e);
                            }
                        });


        CompletableFuture.allOf(iterating, submittingFuture, cancellingFuture ).join();

        assertEquals( 4, result.size() );
        assertIterableEquals( result,  List.of( "e1", "e2", "e3", "e4") );

        System.out.printf("Core pool size: %d%n", executor.getCorePoolSize());
        System.out.printf("Largest pool size: %d%n",executor.getLargestPoolSize());
        System.out.printf("Active threads: %d%n", executor.getActiveCount());
        System.out.printf("Completed tasks: %d%n", executor.getCompletedTaskCount());

    }

}
