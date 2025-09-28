package org.bsc.async;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

public interface AsyncGeneratorBase<E> extends Iterable<E> {

    /**
     * Represents a data element in the AsyncGenerator.
     *
     * @param <E> the type of the data element
     */
    record Data<E> (
            CompletableFuture<E> future,
            AsyncGenerator.Embed<E> embed,
            Object resultValue )
    {

        public boolean isDone() {
            return (future == null && embed == null );
        }

        public boolean isError() {
            return future != null && future.isCompletedExceptionally();
        }

        public static <E> Data<E> of(CompletableFuture<E> future) {
            return new Data<>( requireNonNull(future, "future task cannot be null"), null, null);
        }

        public static <E> Data<E> of(E data) { return new Data<>( completedFuture(data), null, null); }

        public static <E> Data<E> composeWith( AsyncGenerator<E> generator, AsyncGenerator.EmbedCompletionHandler onCompletion) {
            return new Data<>( null, new AsyncGenerator.Embed<>(generator, onCompletion), null );
        }

        public static <E> Data<E> done() { return new Data<>(null, null, null); }

        public static <E> Data<E> done( Object resultValue) { return new Data<>(null, null, resultValue); }

        public static <E> Data<E> error( Throwable exception ) {
            return Data.of(CompletableFuture.failedFuture(exception));
        }

    }

    /**
     * Retrieves the next asynchronous element.
     *
     * @return the next element from the generator
     */
    Data<E> next();


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
                return AsyncGenerator.Data.done(next.resultValue());
            }
            return AsyncGenerator.Data.of(next.future().thenApplyAsync(mapFunction, executor()));
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
                return AsyncGenerator.Data.done(next.resultValue());
            }
            return AsyncGenerator.Data.of(next.future().thenComposeAsync(mapFunction, executor()));
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

                final E value = next.future().join();

                if (predicate.test(value)) {
                    return next;
                }
                next = next();
            }
            return AsyncGenerator.Data.done(next.resultValue());
        };
    }

    /**
     * Asynchronously iterates over the elements of the AsyncGenerator and applies the given consumer to each element.
     *
     * @param consumer the consumer function to be applied to each element
     * @return a CompletableFuture representing the completion of the iteration process.
     */
    default CompletableFuture<Object>   forEachAsync(Consumer<E> consumer) {
/*
        if( this instanceof AsyncGenerator.IsCancellable isCancellable) {
            if (isCancellable.isCancelled()) {
                return completedFuture(AsyncGenerator.IsCancellable.CANCELLED);
            }
        }

 */
        final var next = next();
        if (next.isDone()) {
            return completedFuture(next.resultValue());
        }
        if (next.embed() != null) {
            return next.embed().generator.async(executor()).forEachAsync(consumer)
                    .thenCompose(v -> forEachAsync(consumer) );
        } else {
            return next.future().thenApplyAsync(v -> {
                        consumer.accept(v);
                        return null;
                    }, executor() )
                    .thenComposeAsync(v -> forEachAsync(consumer), executor())
                    ;
        }

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
        final var next = next();
        if (next.isDone()) {
            return completedFuture(result);
        }
        return next.future().thenApplyAsync(v -> {
                    consumer.accept(result, v);
                    return null;
                }, executor() )
                .thenCompose(v -> collectAsync(result, consumer))
                ;

    }

}
