package org.bsc.async;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.completedFuture;

public interface AsyncGenerator<E> extends Iterable<E> {
    class Data<E> {
        final E data;
        final boolean done;

        public Data(E data, boolean done) {
            this.data = data;
            this.done = done;
        }

        static <E> Data<E> of(E data) {
            return new Data<>(data, false);
        }

        static <E> Data<E> done() {
            return new Data<>(null, true);
        }

    }

    CompletableFuture<Data<E>> next();

    default CompletableFuture<Void> forEachAsync(  final AsyncFunction<E,Void> consumer) {

        return next().thenCompose(data -> {
            if (data.done) {
                return completedFuture(null);
            }
            return consumer.apply(data.data)
                    .thenCompose( v -> forEachAsync(consumer) );
        });
    }
    default Stream<E> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
                false);
    }

    default Iterator<E> iterator() {
        return new Iterator<E>() {
            private final AtomicReference<Data<E>> currentFetchedData = new AtomicReference<>();

            {
                currentFetchedData.set(  AsyncGenerator.this.next().join() );
            }
            @Override
            public boolean hasNext() {
                final Data<E> value = currentFetchedData.get();
                return value != null && !value.done;
            }

            @Override
            public E next() {
                Data<E> value = currentFetchedData.get();
                if( value==null || value.done) {
                    throw new IllegalStateException("no more elements into iterator");
                }

                return currentFetchedData.getAndUpdate(  v -> AsyncGenerator.this.next().join() ).data;

            }
        };
    }

}
