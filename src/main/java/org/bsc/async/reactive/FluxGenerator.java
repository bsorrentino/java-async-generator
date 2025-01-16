package org.bsc.async.reactive;

import org.bsc.async.AsyncGenerator;

import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

public interface FluxGenerator {

    static <T, P extends Flow.Publisher<T>> AsyncGenerator<T> fromPublisher( P publisher ) {
        var queue = new LinkedBlockingQueue<AsyncGenerator.Data<T>>();
        return new GeneratorSubscriber<T>( publisher, queue );
    }

    static <T> Flow.Publisher<T> toPublisher( AsyncGenerator<T> generator ) {
        return new GeneratorPublisher<>( generator );
    }
}
