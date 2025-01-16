package org.bsc.async.reactive;

import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;

class GeneratorSubscriber<T> implements Flow.Subscriber<T>, AsyncGenerator<T> {

    private final AsyncGeneratorQueue.Generator<T> delegate;

    public <P extends Flow.Publisher<T>> GeneratorSubscriber(P publisher, BlockingQueue<AsyncGenerator.Data<T>> queue) {
        this.delegate = new AsyncGeneratorQueue.Generator<>( queue );
        publisher.subscribe(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

    }

    @Override
    public void onNext(T item) {
        delegate.queue().add( AsyncGenerator.Data.of( item ) );
    }

    @Override
    public void onError(Throwable error) {
        delegate.queue().add( AsyncGenerator.Data.error(error) );
    }

    @Override
    public void onComplete() {
        delegate.queue().add(AsyncGenerator.Data.done());
    }

    @Override
    public Data<T> next() {
        return delegate.next();
    }
}

