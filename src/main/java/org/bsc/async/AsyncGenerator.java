package org.bsc.async;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * An asynchronous generator interface that allows generating asynchronous elements.
 *
 * @param <E> the type of elements. The generator will emit CompletableFutures&lt;E&gt; elements
 */
public interface AsyncGenerator<E> extends Iterable<E> {
    class Data<E> {
        final CompletableFuture<E> data;
        final boolean done;

        public Data( CompletableFuture<E> data, boolean done) {
            this.data = data;
            this.done = done;
        }

        public static <E> Data<E> of(CompletableFuture<E> data) {
            return new Data<>(data, false);
        }

        public static <E> Data<E> done() {
            return new Data<>(null, true);
        }

    }

    /**
     * Retrieves the next asynchronous element.
     *
     * @return the next element from the generator
     */
    Data<E> next();

    static <E> AsyncGenerator<E> empty() {
        return Data::done;
    }

    static <E,U> AsyncGenerator<U> map(Iterator<E> iterator, Function<E, CompletableFuture<U>> mapFunction ) {
        return () -> {
            if( !iterator.hasNext() ) {
                return Data.done();
            }
            return Data.of(mapFunction.apply( iterator.next() ));
        };
    }
    static <E,U> AsyncGenerator<U> collect(Iterator<E> iterator, BiConsumer<E, Consumer<CompletableFuture<U>>> consumer ) {
        final List<CompletableFuture<U>> accumulator = new ArrayList<>();

        final Consumer<CompletableFuture<U>> addElement = accumulator::add;
        while( iterator.hasNext() ) {
            consumer.accept(iterator.next(), addElement );
        }

        final Iterator<CompletableFuture<U>> it = accumulator.iterator();
        return () -> {
            if( !it.hasNext() ) {
                return Data.done();
            }
            return Data.of(it.next());
        };
    }

    /**
     * Maps elements from a collection to CompletableFuture asynchronously.
     *
     * @param <E> the type of elements in the collection
     * @param <U> the type of elements in the CompletableFuture
     * @param collection the collection of elements to map
     * @param mapFunction the function to map elements to CompletableFuture
     * @return an AsyncGenerator instance with mapped elements
     */
    static <E,U> AsyncGenerator<U> map( Collection<E> collection, Function<E,CompletableFuture<U>> mapFunction ) {
        if( collection == null || collection.isEmpty()) {
            return empty();
        }
        return map( collection.iterator(), mapFunction);
    }
    static <E,U> AsyncGenerator<U> collect( Collection<E> collection, BiConsumer<E, Consumer<CompletableFuture<U>>> consumer ) {
        if( collection == null || collection.isEmpty()) {
            return empty();
        }
        return collect( collection.iterator(), consumer);
    }

    default CompletableFuture<Void>  toCompletableFuture() {
        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenCompose(v -> toCompletableFuture());
    }

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
