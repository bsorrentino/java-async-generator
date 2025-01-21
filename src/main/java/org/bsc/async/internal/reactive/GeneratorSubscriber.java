package org.bsc.async.internal.reactive;

import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;

/**
 * Represents a subscriber for generating asynchronous data streams.
 *
 * <p>This class implements the {@link Flow.Subscriber} and {@link AsyncGenerator} interfaces to handle data flow
 * and produce asynchronous data. It is designed to subscribe to a publisher, process incoming items,
 * and manage error and completion signals.</p>
 *
 * @param <T> The type of elements produced by this generator.
 */
public class GeneratorSubscriber<T> implements Flow.Subscriber<T>, AsyncGenerator<T> {

    private final AsyncGeneratorQueue.Generator<T> delegate;

    /**
     * Constructs a new instance of {@code GeneratorSubscriber}.
     *
     * @param <P> the type of the publisher, which must extend {@link Flow.Publisher}
     * @param publisher the source publisher that will push data to this subscriber
     * @param queue the blocking queue used for storing asynchronous generator data
     */
    public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, BlockingQueue<AsyncGenerator.Data<T>> queue) {
        this.delegate = new AsyncGeneratorQueue.Generator<>( queue );
        publisher.subscribe(this);
    }

    /**
     * Handles the subscription event from a Flux.
     * <p>
     * This method is called when a subscription to the source {@link Flow} has been established.
     * The provided {@code Flow.Subscription} can be used to manage and control the flow of data emissions.
     *
     * @param subscription The subscription object representing this resource owner lifecycle. Used to signal that resources being subscribed to should not be released until this subscription is disposed.
     */
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    /**
     * Passes the received item to the delegated queue as an {@link AsyncGenerator.Data} object.
     *
     * @param item The item to be processed and queued.
     */
    @Override
    public void onNext(T item) {
        delegate.queue().add( AsyncGenerator.Data.of( item ) );
    }

    /**
     * Handles an error by queuing it in the delegate's queue with an errored data.
     *
     * @param error The Throwable that represents the error to be handled.
     */
    @Override
    public void onError(Throwable error) {
        delegate.queue().add( AsyncGenerator.Data.error(error) );
    }

    /**
     * This method is called when the asynchronous operation is completed successfully.
     * It notifies the delegate that no more data will be provided by adding a done marker to the queue.
     */
    @Override
    public void onComplete() {
        delegate.queue().add(AsyncGenerator.Data.done());
    }

    /**
     * Returns the next {@code Data<T>} object from this iteration.
     * 
     * @return the next element in the iteration, or null if there is no such element
     */
    @Override
    public Data<T> next() {
        return delegate.next();
    }
}