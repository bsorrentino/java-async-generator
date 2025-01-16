package org.bsc.async.reactive;

import org.bsc.async.AsyncGenerator;

import java.util.concurrent.Flow;

class GeneratorPublisher<T> implements Flow.Publisher<T> {

    private final AsyncGenerator<? extends T> delegate;

    public GeneratorPublisher(AsyncGenerator<? extends T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                throw new UnsupportedOperationException("cancel is not implemented yet!");
            }
        });

        delegate.forEachAsync(subscriber::onNext)
                .thenAccept( value -> {
                    subscriber.onComplete();
                })
                .exceptionally( ex -> {
                    subscriber.onError(ex);
                    return null;
                })
                .join();
    }

}
