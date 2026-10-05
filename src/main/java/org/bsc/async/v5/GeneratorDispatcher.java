package org.bsc.async.v5;

import org.bsc.async.AsyncGenerator.Data;

import static java.util.Objects.requireNonNull;

final class GeneratorDispatcher<E> implements AsyncGeneratorFlow.Dispatcher<E> {
    private final AsyncGeneratorFlow.Dispatcher<E> delegate;
    private final AsyncGeneratorFlow.Generator<E> generator;

    GeneratorDispatcher(AsyncGeneratorFlow.Dispatcher<E> delegate, AsyncGeneratorFlow.Generator<E> generator) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.generator = requireNonNull(generator, "generator cannot be null");
    }

    @Override
    public void dispatchSync(Data<E> data) throws InterruptedException {
        if( generator.isCancelled() ) {
            throw new InterruptedException("dispatchSync cancelled");
        }
        generator.cancellableExecutor.callUntilCancelled(() -> {
            delegate.dispatchSync(data);
            return null;
        });
    }

    @Override
    public void dispatchAsync(Data<E> data) {
        if( !generator.isCancelled() ) {
            delegate.dispatchAsync(data);
        }
    }

    @Override
    public boolean isCancelled() {
        return generator.isCancelled();
    }

    @Override
    public Registration onCancel(Listener listener) {
        return generator.onCancel(listener);
    }
}
