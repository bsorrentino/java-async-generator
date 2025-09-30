package org.bsc.async;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
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


    Executor executor();


    class Mapper<E,U> extends AsyncGenerator.Base<U> implements AsyncGenerator.Cancellable<U>, AsyncGenerator.HasResultValue {

        protected final AsyncGeneratorBase<E> delegate;
        final Function<E, U> mapFunction;
        private Object resultValue;

        protected Mapper(AsyncGeneratorBase<E> delegate, Function<E, U> mapFunction) {
            this.delegate = requireNonNull(delegate, "delegate cannot be null");
            this.mapFunction = requireNonNull(mapFunction, "mapFunction cannot be null");

        }

        @Override
        public final Executor executor() {
            return delegate.executor();
        }

        /**
         * Retrieves the result value of the generator, if any.
         *
         * @return an {@link Optional} containing the result value if present, or an empty Optional if not
         */
        public Optional<Object> resultValue() { return ofNullable(resultValue); };

        @Override
        public final Data<U> next() {
            final Data<E> next = ( isCancelled() ) ? Data.done(CANCELLED) : delegate.next();

            if( next.isDone() ) {
                resultValue = next.resultValue();
                return Data.done();
            }
            return Data.of(next.future().thenApply(mapFunction));
        }

        @Override
        public boolean isCancelled() {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.isCancelled();
            }
            return false;
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.cancel( mayInterruptIfRunning );
            }
            return false;
        }
    }


    /**
     * Maps the elements of this generator to a new asynchronous generator.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped elements
     */
    default <U> AsyncGenerator<U> map(Function<E, U> mapFunction) {
        return new Mapper<>( this, mapFunction );
    }


    class FlatMapper<E,U> extends AsyncGenerator.Base<U> implements AsyncGenerator.Cancellable<U>, AsyncGenerator.HasResultValue {

        protected final AsyncGeneratorBase<E> delegate;
        final Function<E, CompletableFuture<U>> mapFunction;
        private Object resultValue;

        protected FlatMapper(AsyncGeneratorBase<E> delegate, Function<E, CompletableFuture<U>> mapFunction) {
            this.delegate = requireNonNull(delegate, "delegate cannot be null");
            this.mapFunction = requireNonNull(mapFunction, "mapFunction cannot be null");

        }

        @Override
        public final Executor executor() {
            return delegate.executor();
        }

        /**
         * Retrieves the result value of the generator, if any.
         *
         * @return an {@link Optional} containing the result value if present, or an empty Optional if not
         */
        public Optional<Object> resultValue() { return ofNullable(resultValue); };

        @Override
        public final Data<U> next() {
            final Data<E> next = ( isCancelled() ) ? Data.done(CANCELLED) : delegate.next();

            if( next.isDone() ) {
                resultValue = next.resultValue();
                return Data.done();
            }
            return Data.of(next.future().thenCompose(mapFunction));
        }

        @Override
        public boolean isCancelled() {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.isCancelled();
            }
            return false;
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.cancel( mayInterruptIfRunning );
            }
            return false;
        }
    }

    /**
     * Maps the elements of this generator to a new asynchronous generator, and flattens the resulting nested generators.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped and flattened elements
     */
    default <U> AsyncGenerator<U> flatMap(Function<E, CompletableFuture<U>> mapFunction) {
        return new FlatMapper<>( this, mapFunction );
    }

    private CompletableFuture<Object> forEachSync(Consumer<E> consumer) {
        final var next = next();
        if (next.isDone()) {
            return completedFuture(next.resultValue());
        }
        if (next.embed() != null) {
            return next.embed().generator.forEachAsync(consumer)
                    .thenCompose(v -> forEachSync(consumer))
                    ;
        } else {
            return next.future()
                    .thenApply(v -> {
                        consumer.accept(v);
                        return null;
                    })
                    .thenCompose(v -> forEachSync(consumer))
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
        return CompletableFuture.supplyAsync( () -> forEachSync( consumer ), executor() ).join();
    }

    private <R> CompletableFuture<R> reduceSync(R result, BiFunction<R,E,R> reducer) {
        final var next = next();
        if (next.isDone()) {
            return completedFuture(result);
        }
        return next.future()
                .thenApplyAsync(v -> reducer.apply(result, v), executor() )
                .thenCompose(v -> reduceSync(result, reducer))
                ;

    }

    /**
     * Collects elements from the AsyncGenerator asynchronously into a list.
     *
     * @param <R>      the type of the result list
     * @param result   the result list to collect elements into
     * @param reducer the reducer function for processing elements
     * @return a CompletableFuture representing the completion of the collection process
     */
    default <R> CompletableFuture<R> reduceAsync(R result, BiFunction<R,E,R> reducer) {
        return CompletableFuture.supplyAsync( () -> reduceSync( result, reducer ), executor() ).join();
    }

}
