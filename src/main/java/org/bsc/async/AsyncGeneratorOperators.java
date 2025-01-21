package org.bsc.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public interface AsyncGeneratorOperators<E> {

    AsyncGenerator.Data<E> next();

    default Executor executor() {
        return Runnable::run;
    }

    /**
     * Maps the elements of this generator to a new asynchronous generator.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped elements
     */
    default <U> AsyncGenerator<U> map(Function<E, U> mapFunction) {
        return () -> {
            final AsyncGenerator.Data<E> next = next();
            if (next.isDone()) {
                return AsyncGenerator.Data.done(next.resultValue);
            }
            return AsyncGenerator.Data.of(next.data.thenApplyAsync(mapFunction, executor()));
        };
    }

    /**
     * Maps the elements of this generator to a new asynchronous generator, and flattens the resulting nested generators.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped and flattened elements
     */
    default <U> AsyncGenerator<U> flatMap(Function<E, CompletableFuture<U>> mapFunction) {
        return () -> {
            final AsyncGenerator.Data<E> next = next();
            if (next.isDone()) {
                return AsyncGenerator.Data.done(next.resultValue);
            }
            return AsyncGenerator.Data.of(next.data.thenComposeAsync(mapFunction, executor()));
        };
    }

    /**
     * Filters the elements of this generator based on the given predicate.
     * Only elements that satisfy the predicate will be included in the resulting generator.
     *
     * @param predicate the predicate to test elements against
     * @return a generator with elements that satisfy the predicate
     */
    default AsyncGenerator<E> filter(Predicate<E> predicate) {
        return () -> {
            AsyncGenerator.Data<E> next = next();
            while (!next.isDone()) {

                final E value = next.data.join();

                if (predicate.test(value)) {
                    return next;
                }
                next = next();
            }
            return AsyncGenerator.Data.done(next.resultValue);
        };
    }

    /**
     * Asynchronously iterates over the elements of the AsyncGenerator and applies the given consumer to each element.
     * This method doesn't spawn new threads.
     *
     * @param consumer the consumer function to be applied to each element
     * @return a CompletableFuture representing the completion of the iteration process.
     */
    private CompletableFuture<Object> forEachAsyncNested(Consumer<E> consumer) {
        final var next = next();
        if (next.isDone()) {
            return completedFuture(next.resultValue);
        }
        if (next.embed != null) {
            return next.embed.generator.forEachAsync(consumer)
                    .thenCompose(v -> forEachAsyncNested(consumer));
        } else {
            return next.data.thenApply(v -> {
                        consumer.accept(v);
                        return null;
                    })
                    .thenCompose(v -> forEachAsyncNested(consumer))
                    ;
        }
    }

    /**
     * Asynchronously iterates over the elements of the AsyncGenerator and applies the given consumer to each element.
     *
     * @param consumer the consumer function to be applied to each element
     * @return a CompletableFuture representing the completion of the iteration process.
     */
    default CompletableFuture<Object> forEachAsync(Consumer<E> consumer) {
        return supplyAsync( () -> {
            final var next = next();
            if (next.isDone()) {
                return next.resultValue;
            }
            if (next.embed != null) {
                return next.embed.generator.forEachAsync(consumer)
                        .thenCompose(v -> forEachAsyncNested(consumer));
            } else {
                return next.data.thenApply(v -> {
                            consumer.accept(v);
                            return null;
                        })
                        .thenCompose(v -> forEachAsyncNested(consumer))
                        .join();
            }
        }, executor());
    }


    /**
     *  Collects elements from the AsyncGenerator into a list.
     *  This method doesn't spawn new threads.
     *
     * @param <R>      the type of the result list
     * @param result   the result list to collect elements into
     * @param consumer the consumer function for processing elements
     * @return a CompletableFuture representing the completion of the collection process
     */
    private <R extends List<E>> CompletableFuture<R> collectAsyncNested(R result, BiConsumer<R,E> consumer) {

        final var next = next();
        if (next.isDone()) {
            return completedFuture(result);
        }
        return next.data.thenApply(v -> {
                    consumer.accept(result, v);
                    return null;
                })
                .thenCompose(v -> collectAsyncNested(result, consumer))
                ;

    }

    /**
     * Collects elements from the AsyncGenerator asynchronously into a list.
     *
     * @param <R>      the type of the result list
     * @param result   the result list to collect elements into
     * @param consumer the consumer function for processing elements
     * @return a CompletableFuture representing the completion of the collection process
     */
    default <R extends List<E>> CompletableFuture<R> collectAsync(R result, BiConsumer<R,E> consumer) {

        return supplyAsync( () -> {

            final var next = next();
            if (next.isDone()) {
                return result;
            }
            return next.data.thenApply(v -> {
                        consumer.accept(result, v);
                        return null;
                    })
                    .thenCompose(v -> collectAsyncNested(result, consumer))
                    .join();

        }, executor());
    }

}
