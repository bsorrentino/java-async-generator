package org.bsc.async;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.completedFuture;

public interface AsyncGenerator<E> extends Iterable<E> {
    class Data<E> {
        final CompletableFuture<E> data;
        final boolean done;

        public Data( CompletableFuture<E> data, boolean done) {
            this.data = data;
            this.done = done;
        }

        static <E> Data<E> of(CompletableFuture<E> data) {
            return new Data<>(data, false);
        }

        static <E> Data<E> done() {
            return new Data<>(null, true);
        }

    }

    Data<E> next();

    default CompletableFuture<Void> forEachAsync( Consumer<E> consumer) {

        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenApply( v -> {
                            consumer.accept(v);
                            return null;
                        })
                        .thenCompose(v -> forEachAsync(consumer));
    }
    default <R extends List<E>> CompletableFuture<R> collectAsync(R result, Consumer<E> consumer) {

        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenApply( v -> {
                        consumer.accept(v);
                        result.add(v);
                        return null;
                    })
                    .thenCompose(v -> collectAsync( result, consumer));
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

                currentFetchedData.set(  AsyncGenerator.this.next() );
            }
            @Override
            public boolean hasNext() {
                final Data<E> value = currentFetchedData.get();
                return value != null && !value.done;
            }

            @Override
            public E next() {
                Data<E> next = currentFetchedData.get();
                if( next==null || next.done) {
                    throw new IllegalStateException("no more elements into iterator");
                }

                next = currentFetchedData.getAndUpdate(  v -> AsyncGenerator.this.next() );

                return next.data.join();

            }
        };
    }

}
