package org.bsc.async;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
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
 * @param <E> the type of elements. The generator will emit {@link  java.util.concurrent.CompletableFuture CompletableFutures&lt;E&gt;} elements
 */
public interface AsyncGenerator<E> extends Iterable<E> {

    /**
     * Represents a data element in the AsyncGenerator.
     *
     * @param <E> the type of the data element
     */
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


    /**
     * Returns an empty AsyncGenerator.
     *
     * @param <E> the type of elements
     * @return an empty AsyncGenerator
     */
    static <E> AsyncGenerator<E> empty() {
        return Data::done;
    }

    /**
     * create a generator, mapping each element  to an asynchronous counterpart.
     *
     * @param <E> the type of elements in the collection
     * @param <U> the type of elements in the CompletableFuture
     * @param iterator the elements iterator
     * @param mapFunction the function to map elements to {@link  java.util.concurrent.CompletableFuture}
     * @return an AsyncGenerator instance with mapped elements
     */
    static <E,U> AsyncGenerator<U> map(Iterator<E> iterator, Function<E, CompletableFuture<U>> mapFunction ) {
        return () -> {
            if( !iterator.hasNext() ) {
                return Data.done();
            }
            return Data.of(mapFunction.apply( iterator.next() ));
        };
    }

    /**
     * Collects asynchronous elements from an iterator.
     *
     * @param <E> the type of elements in the iterator
     * @param <U> the type of elements in the CompletableFuture
     * @param iterator the iterator containing elements to collect
     * @param consumer the function to consume elements and add them to the accumulator
     * @return an AsyncGenerator instance with collected elements
     */
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
     * create a generator, mapping each element  to an asynchronous counterpart.
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
    /**
     * Collects asynchronous elements from a collection.
     *
     * @param <E> the type of elements in the iterator
     * @param <U> the type of elements in the CompletableFuture
     * @param collection the iterator containing elements to collect
     * @param consumer the function to consume elements and add them to the accumulator
     * @return an AsyncGenerator instance with collected elements
     */
    static <E,U> AsyncGenerator<U> collect( Collection<E> collection, BiConsumer<E, Consumer<CompletableFuture<U>>> consumer ) {
        if( collection == null || collection.isEmpty()) {
            return empty();
        }
        return collect( collection.iterator(), consumer);
    }

    /**
     * Converts the AsyncGenerator to a CompletableFuture.
     *
     * @return a CompletableFuture representing the completion of the AsyncGenerator
     */
    default CompletableFuture<Void>  toCompletableFuture() {
        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenCompose(v -> toCompletableFuture());
    }

    /**
     * Asynchronously iterates over the elements of the AsyncGenerator and applies the given consumer to each element.
     *
     * @param consumer the consumer function to be applied to each element
     * @param executor the executor to use for the asynchronous iteration
     * @return a CompletableFuture representing the completion of the iteration process
     */
    default CompletableFuture<Void> forEachAsync( Consumer<E> consumer, Executor executor) {

        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenApplyAsync( v -> {
                            consumer.accept(v);
                            return null;
                        }, executor)
                        .thenCompose(v -> forEachAsync(consumer, executor));
    }

    /**
     * Asynchronously iterates over the elements of the AsyncGenerator and applies the given consumer to each element.
     *
     * @param consumer the consumer function to be applied to each element
     * @return a CompletableFuture representing the completion of the iteration process
     */
    default CompletableFuture<Void> forEachAsync( Consumer<E> consumer) {
        return forEachAsync( consumer, ForkJoinPool.commonPool());
    }

    /**
     * Collects elements from the AsyncGenerator asynchronously into a list.
     *
     * @param <R> the type of the result list
     * @param result the result list to collect elements into
     * @param consumer the consumer function for processing elements
     * @param executor the executor to use for the asynchronous collection
     * @return a CompletableFuture representing the completion of the collection process
     */
    default <R extends List<E>> CompletableFuture<R> collectAsync(R result, Consumer<E> consumer, Executor executor) {

        final Data<E> next = next();
        if( next.done ) {
            return completedFuture(null);
        }
        return next.data.thenApplyAsync( v -> {
                    consumer.accept(v);
                    result.add(v);
                    return null;
                }, executor )
                .thenCompose(v -> collectAsync( result, consumer, executor ));
    }

    /**
     * Collects elements from the AsyncGenerator asynchronously into a list.
     *
     * @param <R> the type of the result list
     * @param result the result list to collect elements into
     * @param consumer the consumer function for processing elements
     * @return a CompletableFuture representing the completion of the collection process
     */
    default <R extends List<E>> CompletableFuture<R> collectAsync(R result, Consumer<E> consumer) {
        return collectAsync( result, consumer, ForkJoinPool.commonPool());
    }
    /**
     * Returns a sequential Stream with the elements of this AsyncGenerator.
     * Each CompletableFuture is resolved and then make available to the stream.
     *
     * @return a Stream of elements from the AsyncGenerator
     */
    default Stream<E> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
                false);
    }
    /**
     * Returns an iterator over the elements of this AsyncGenerator.
     * Each call to `next` retrieves the next "resolved" asynchronous element from the generator.
     *
     * @return an iterator over the elements of this AsyncGenerator
     */
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
