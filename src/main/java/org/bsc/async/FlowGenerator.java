package org.bsc.async;

import org.bsc.async.internal.reactive.GeneratorPublisher;
import org.bsc.async.internal.reactive.GeneratorSubscriber;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provides methods for converting between {@link FlowGenerator} and various {@link java.util.concurrent.Flow.Publisher} types.
 *
 * @since 3.0
 */
public interface FlowGenerator {

    /**
     * Creates an {@code AsyncGenerator} from a {@code Flow.Publisher}.
     *
     * @param <T> the type of item emitted by the publisher
     * @param <P> the type of the publisher
     * @param publisher the publisher to subscribe to for retrieving items asynchronously
     * @return an {@code AsyncGenerator} that emits items from the publisher
     */
    static <T, P extends Flow.Publisher<T>> AsyncGenerator<T> fromPublisher( P publisher ) {
        var queue = new LinkedBlockingQueue<AsyncGenerator.Data<T>>();
        return new GeneratorSubscriber<T>( publisher, queue );
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