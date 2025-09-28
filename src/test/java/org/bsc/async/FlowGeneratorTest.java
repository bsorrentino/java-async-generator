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

        var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        var publisher = new SubmissionPublisher<String>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5" );

        var generator = FlowGenerator.fromPublisher(publisher);

        assertTrue( publisher.hasSubscribers() );

        var submitting = runAsync( () -> {
                data.stream().peek(System.out::println).forEach( publisher::submit );
                publisher.close();
            }, executor );

        final List<String> result = new ArrayList<>();
        var iterating = generator.async( executor ).forEachAsync(result::add);

        CompletableFuture.allOf(iterating, submitting ).join();

        assertEquals( data.size(), result.size() );
        assertIterableEquals( data, result );

        System.out.println("Core pool size: " + executor.getCorePoolSize());
        System.out.println("Largest pool size: " + executor.getLargestPoolSize());
        System.out.println("Active threads: " + executor.getActiveCount());
        System.out.println("Completed tasks: " + executor.getCompletedTaskCount());

    }

    @Test
    public void flowGeneratorPublisherTest() throws Exception {

        var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        var queue = new LinkedBlockingQueue<AsyncGenerator.Data<String>>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5" );

        var generator = AsyncGeneratorQueue.of( queue, q -> {

            for( String value: data ) {
                queue.add(AsyncGenerator.Data.of(completedFuture(value)));
            }
        }, executor);

        var publisher = FlowGenerator.toPublisher( generator );

        var result = new ArrayList<String>();

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

        var executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

        var publisher = new SubmissionPublisher<String>();

        final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );

        var generator = FlowGenerator.fromPublisher(publisher);

        assertTrue( publisher.hasSubscribers() );

        var submitting = runAsync( () -> {

            try {
                for (String value : data) {
                    System.out.printf("publishing: %s\n", value);
                    publisher.submit(value);

                    Thread.sleep(1000);
                }
            } catch( InterruptedException e ) {
                throw new CompletionException(e);
            } finally{
                publisher.close();

            }
        }, executor );

        final List<String> result = new ArrayList<>();
        var iterating = generator
                        .async( executor )
                        .forEachAsync( value -> {
                            try {
                                Thread.sleep(10);
                                System.out.printf("received: %s\n", value);
                                result.add(value);
                            } catch (InterruptedException e) {
                                throw new CompletionException(e);
                            }
                        });


        CompletableFuture.allOf(iterating, submitting );

        Thread.sleep( 4000 );
        generator.cancel();


        assertEquals( 4, result.size() );
        assertIterableEquals( result,  List.of( "e1", "e2", "e3", "e4") );

        System.out.println("Core pool size: " + executor.getCorePoolSize());
        System.out.println("Largest pool size: " + executor.getLargestPoolSize());
        System.out.println("Active threads: " + executor.getActiveCount());
        System.out.println("Completed tasks: " + executor.getCompletedTaskCount());

    }

}
