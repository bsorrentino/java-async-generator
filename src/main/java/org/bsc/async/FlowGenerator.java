package org.bsc.async;

import org.bsc.async.internal.reactive.GeneratorPublisher;
import org.bsc.async.internal.reactive.GeneratorSubscriber;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Provides methods for converting between {@link FlowGenerator} and various {@link java.util.concurrent.Flow.Publisher} types.
 *
 * @since 3.0.0
 */
public interface FlowGenerator {

    /**
     * Creates an {@code AsyncGenerator} from a {@code Flow.Publisher}.
     *
     * @param <T> the type of item emitted by the publisher
     * @param <P> the type of the publisher
     * @param publisher the publisher to subscribe to for retrieving items asynchronously
     * @param mapResult function that will set generator's result
     * @return an {@code AsyncGenerator} that emits items from the publisher
     */
    @SuppressWarnings("unchecked")
    static <T, P extends Flow.Publisher<T>, R> AsyncGenerator.Cancellable<T> fromPublisher( P publisher, Supplier<R> mapResult ) {
        var queue = new LinkedBlockingQueue<AsyncGenerator.Data<T>>();
        return new GeneratorSubscriber<>( publisher, (Supplier<Object>) mapResult, queue );
    }

    /**
     * Creates an {@code AsyncGenerator} from a {@code Flow.Publisher}.
     *
     * @param <T> the type of item emitted by the publisher
     * @param <P> the type of the publisher
     * @param publisher the publisher to subscribe to for retrieving items asynchronously
     * @return an {@code AsyncGenerator} that emits items from the publisher
     */
    static <T, P extends Flow.Publisher<T>> AsyncGenerator.Cancellable<T> fromPublisher( P publisher ) {
        return fromPublisher( publisher, null );
    }

    /**
     * Converts an {@code AsyncGenerator} into a {@code Flow.Publisher}.
     *
     * @param <T> the type of elements emitted by the publisher
     * @param generator the async generator to convert
     * @return a flow publisher
     */
    static <T> Flow.Publisher<T> toPublisher( AsyncGenerator<T> generator ) {
        return new GeneratorPublisher<>( generator );
    }
}