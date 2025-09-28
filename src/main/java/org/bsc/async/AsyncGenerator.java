package org.bsc.async;

import org.bsc.async.internal.UnmodifiableDeque;

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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * An asynchronous generator interface that allows generating asynchronous elements.
 *
 * @param <E> the type of elements. The generator will emit {@link  java.util.concurrent.CompletableFuture CompletableFutures&lt;E&gt;} elements
 */
public interface AsyncGenerator<E> extends AsyncGeneratorBase<E> {

    interface HasResultValue {

        Optional<Object> resultValue();
    }

    interface Cancellable<E> extends AsyncGenerator<E> {
        Object CANCELLED = new Object()  {
            @Override
            public String toString() {
                return "CANCELLED";
            }
        };

        /**
         * Checks if the asynchronous generation has been cancelled.
         * <p>
         * The default implementation always returns {@code false}.
         * Implementations that support cancellation should override this method.
         *
         * @return {@code true} if the generator has been cancelled, {@code false} otherwise.
         */
        boolean isCancelled();

        /**
         * method that request to cancel generator
         */
        boolean cancel();


    }
    static Optional<Object> resultValue( AsyncGenerator<?> generator ) {
        if( generator instanceof HasResultValue withResult ) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }

    static Optional<Object> resultValue( Iterator<?> iterator ) {
        if( iterator instanceof HasResultValue withResult ) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }

    /**
     * An asynchronous generator decorator that allows retrieving the result value of the asynchronous operation, if any.
     *
     * @param <E> the type of elements in the generator
     */
    class WithResult<E> implements AsyncGenerator.Cancellable<E>, HasResultValue {

        protected final AsyncGenerator<E> delegate;
        private Object resultValue;

        public WithResult(AsyncGenerator<E> delegate) {
            this.delegate = delegate;
        }

        public AsyncGenerator<E> delegate() { return delegate; }

        /**
         * Retrieves the result value of the generator, if any.
         *
         * @return an {@link Optional} containing the result value if present, or an empty Optional if not
         */
        public Optional<Object> resultValue() { return ofNullable(resultValue); };

        @Override
        public final Data<E> next() {
            final Data<E> result = ( isCancelled() ) ? Data.done(CANCELLED) : delegate.next();

            if( result.isDone() ) {
                resultValue = result.resultValue();
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.isCancelled();
            }
            return false;
        }

        @Override
        public boolean cancel() {
            if( delegate instanceof Cancellable<?> isCancellable ) {
                return isCancellable.cancel();
            }
            return false;
        }
    }

    /**
     * An asynchronous generator decorator that allows to generators composition embedding other generators.
     *
     * @param <E> the type of elements in the generator
     */
    class WithEmbed<E> extends AbstractCancellableAsyncGenerator<E> implements  HasResultValue {
        protected final Deque<Embed<E>> generatorsStack = new ArrayDeque<>(2);
        private final Deque<Data<E>> returnValueStack = new ArrayDeque<>(2);

        public WithEmbed(AsyncGenerator<E> delegate, EmbedCompletionHandler onGeneratorDoneWithResult) {
            generatorsStack.push( new Embed<>(delegate, onGeneratorDoneWithResult) );
        }
        public WithEmbed(AsyncGenerator<E> delegate ) {
            this(delegate, null);
        }

        public Deque<Data<E>> resultValues() {
            return new UnmodifiableDeque<>( returnValueStack );
        }

        public Optional<Object> resultValue() {
            return ofNullable( returnValueStack.peek() )
                        .map(Data::resultValue);
        }

        private void clearPreviousReturnsValuesIfAny() {
            // Check if the return values are which ones from previous run
            if( returnValueStack.size() > 1 && returnValueStack.size() == generatorsStack.size() ) {
                returnValueStack.clear();
            }
        }

//        private AsyncGenerator.WithResult<E> toGeneratorWithResult( AsyncGenerator<E> generator ) {
//            return ( generator instanceof WithResult ) ?
//                    (AsyncGenerator.WithResult<E>) generator :
//                    new WithResult<>(generator);
//        }

        protected boolean isLastGenerator() {
            return generatorsStack.size() == 1;
        }

        @Override
        public Data<E> next() {
            if( generatorsStack.isEmpty() ) { // GUARD
                throw new IllegalStateException("no generator found!");
            }

            final Embed<E> embed = generatorsStack.peek();
            final Data<E> result;
            if( isCancelled() ) {
                if( embed.generator instanceof Cancellable<?> isCancellable && !isCancellable.isCancelled() ) {
                    isCancellable.cancel();
                }
                result =  Data.done(CANCELLED);
            }
            else  {
                result = embed.generator.next();
            }

            if( result.isDone() ) {
                clearPreviousReturnsValuesIfAny();
                returnValueStack.push( result );
                if( embed.onCompletion != null /* && result.resultValue != null */ ) {
                    try {
                        embed.onCompletion.accept( result.resultValue() );
                    } catch (Exception e) {
                        return Data.error(e);
                    }
                }
                if( isLastGenerator() ) {
                    return result;
                }
                generatorsStack.pop();
                return next();
            }
            if( result.embed() != null ) {
                if( generatorsStack.size() >= 2 ) {
                    return Data.error(new UnsupportedOperationException("Currently recursive nested generators are not supported!"));
                }
                generatorsStack.push( result.embed() );
                return next();
            }

            return result;
        }

        @Override
        public boolean cancel() {
            var result = false;
            for( var embed : generatorsStack ) {
                if( embed.generator instanceof Cancellable<?> isCancellable ) {
                    result = result || isCancellable.cancel();
                }
            }
            return result;
        }
    }

    @FunctionalInterface
    interface EmbedCompletionHandler  {
        void accept(Object t) throws Exception;
    }

    class Embed<E> implements HasResultValue {
        final AsyncGenerator<E> generator;
        final EmbedCompletionHandler onCompletion;

        public Embed(AsyncGenerator<E> generator, EmbedCompletionHandler onCompletion) {
            requireNonNull(generator, "generator cannot be null");
            this.generator = generator;
            this.onCompletion = onCompletion;
        }

        @Override
        public Optional<Object> resultValue() {
            return AsyncGenerator.resultValue(generator);
        };
    }

    /**
     * return an async generator that use the given executor
     * @param executor the executor to use
     * @return new async generator
     */
    default AsyncGeneratorBase<E> async( Executor executor ) {
        return new AsyncGeneratorBase<>() {
            @Override
            public Iterator<E> iterator() {
                return AsyncGenerator.this.iterator();
            }

            @Override
            public Data<E> next() {
                return AsyncGenerator.this.next();
            }

            @Override
            public Executor executor() {
                return executor;
            }
        };
    }

    /**
     * return an async generator that use the ForkJoinPool.commonPool() as default executor
     * @return new async generator
     */
    default AsyncGeneratorBase<E> async() {
        return async(ForkJoinPool.commonPool());
    }

    /**
     * Converts the AsyncGenerator to a CompletableFuture.
     *
     * @return a CompletableFuture representing the completion of the AsyncGenerator
     */
    default CompletableFuture<Object>  toCompletableFuture() {
        final Data<E> next = next();
        if( next.isDone() ) {
            return completedFuture(next.resultValue());
        }
        return next.future().thenCompose(v -> toCompletableFuture());
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
        return new InternalIterator<E>( this );
    }


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

}

class InternalIterator<E> implements Iterator<E>, AsyncGenerator.HasResultValue {
    private final AsyncGenerator<E> delegate;

    final AtomicReference<AsyncGenerator.Data<E>> currentFetchedData;

    public InternalIterator(AsyncGenerator<E> delegate) {
        this.delegate = delegate;
        currentFetchedData = new AtomicReference<>(delegate.next());
    }
    @Override
    public boolean hasNext() {
        final var value = currentFetchedData.get();
        return value != null && !value.isDone();
    }

    @Override
    public E next() {
        var next = currentFetchedData.get();

        if( next==null || next.isDone() ) {
            throw new IllegalStateException("no more elements into iterator");
        }

        if( !next.isError() ) {
            currentFetchedData.set( delegate.next() );
        }

        return next.future().join();
    }

    @Override
    public Optional<Object> resultValue() {
        if( delegate instanceof AsyncGenerator.HasResultValue withResult ) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }
};
