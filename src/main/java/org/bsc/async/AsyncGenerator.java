package org.bsc.async;

import org.bsc.async.internal.UnmodifiableDeque;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * An asynchronous generator interface that allows generating asynchronous elements.
 *
 * @param <E> the type of elements. The generator will emit {@link  java.util.concurrent.CompletableFuture CompletableFutures&lt;E&gt;} elements
 */
public interface AsyncGenerator<E> extends Iterable<E> {

    interface HasResultValue {

        Optional<Object> resultValue();
    }

    interface Cancellable<E> extends AsyncGenerator<E> {
        Object CANCELLED = new Object() {
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
        boolean cancel(boolean mayInterruptIfRunning);


    }

    static Optional<Object> resultValue(AsyncGenerator<?> generator) {
        if (generator instanceof HasResultValue withResult) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }

    static Optional<Object> resultValue(Iterator<?> iterator) {
        if (iterator instanceof HasResultValue withResult) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }

    abstract class Base<E> implements AsyncGenerator<E> {

        private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable ->
                new Thread(runnable, format("AsyncGenerator[%d]", hashCode())));

        @Override
        public Executor executor() {
            return executor;
        }

    }

    abstract class BaseCancellable<E> extends Base<E> implements Cancellable<E> {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (cancelled.compareAndSet(false, true)) {
                if (executor() instanceof ExecutorService service) {
                    if (mayInterruptIfRunning && !service.isShutdown() && !service.isTerminated()) {
                        service.shutdownNow();
                    }
                }
                return true;
            }
            return false;
        }

    }

    /**
     * An asynchronous generator decorator that allows retrieving the result value of the asynchronous operation, if any.
     *
     * @param <E> the type of elements in the generator
     */
    class WithResult<E> extends Base<E> implements Cancellable<E>, HasResultValue {

        protected final AsyncGenerator<E> delegate;
        private Object resultValue;

        public WithResult(AsyncGenerator<E> delegate) {
            this.delegate = delegate;
        }

        public AsyncGenerator<E> delegate() {
            return delegate;
        }

        @Override
        public Executor executor() {
            return delegate.executor();
        }

        /**
         * Retrieves the result value of the generator, if any.
         *
         * @return an {@link Optional} containing the result value if present, or an empty Optional if not
         */
        public Optional<Object> resultValue() {
            return ofNullable(resultValue);
        }

        @Override
        public Data<E> next() {
            final Data<E> result = (isCancelled()) ? Data.done(CANCELLED) : delegate.next();

            if (result.isDone()) {
                resultValue = result.resultValue();
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            if (delegate instanceof Cancellable<?> isCancellable) {
                return isCancellable.isCancelled();
            }
            return false;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (delegate instanceof Cancellable<?> isCancellable) {
                return isCancellable.cancel(mayInterruptIfRunning);
            } else if (mayInterruptIfRunning) {
                if (delegate.executor() instanceof ExecutorService service) {
                    if (!(service.isShutdown() || service.isTerminated())) {
                        service.shutdownNow();
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * An asynchronous generator decorator that allows to generators composition embedding other generators.
     *
     * @param <E> the type of elements in the generator
     */
    class WithEmbed<E> extends BaseCancellable<E> implements HasResultValue {
        protected final Deque<Embed<E>> generatorsStack = new ArrayDeque<>(2);
        private final Deque<Data<E>> returnValueStack = new ArrayDeque<>(2);

        public WithEmbed(AsyncGenerator<E> delegate, EmbedCompletionHandler onGeneratorDoneWithResult) {
            generatorsStack.push(new Embed<>(delegate, onGeneratorDoneWithResult));
        }

        public WithEmbed(AsyncGenerator<E> delegate) {
            this(delegate, null);
        }

        @Override
        public final Executor executor() {
            if (generatorsStack.isEmpty()) {
                throw new IllegalStateException("no generator found!");
            }
            return generatorsStack.peek().generator.executor();
        }

        public Deque<Data<E>> resultValues() {
            return new UnmodifiableDeque<>(returnValueStack);
        }

        public Optional<Object> resultValue() {
            return ofNullable(returnValueStack.peek())
                    .map(Data::resultValue);
        }

        private void clearPreviousReturnsValuesIfAny() {
            // Check if the return values are which ones from previous run
            if (returnValueStack.size() > 1 && returnValueStack.size() == generatorsStack.size()) {
                returnValueStack.clear();
            }
        }

        protected boolean isLastGenerator() {
            return generatorsStack.size() == 1;
        }

        @Override
        public Data<E> next() {
            if (generatorsStack.isEmpty()) { // GUARD
                throw new IllegalStateException("no generator found!");
            }

            final Embed<E> embed = generatorsStack.peek();
            final Data<E> result;
            if (isCancelled()) {
                if (embed.generator instanceof Cancellable<?> isCancellable && !isCancellable.isCancelled()) {
                    isCancellable.cancel(false);
                }
                result = Data.done(CANCELLED);
            } else {
                result = embed.generator.next();
            }

            if (result.isDone()) {
                clearPreviousReturnsValuesIfAny();
                returnValueStack.push(result);
                if (embed.onCompletion != null /* && result.resultValue != null */) {
                    try {
                        embed.onCompletion.accept(result.resultValue());
                    } catch (Exception e) {
                        return Data.error(e);
                    }
                }
                if (isLastGenerator()) {
                    return result;
                }
                generatorsStack.pop();
                return next();
            }
            if (result.embed() != null) {
                if (generatorsStack.size() >= 2) {
                    return Data.error(new UnsupportedOperationException("Currently recursive nested generators are not supported!"));
                }
                generatorsStack.push(result.embed());
                return next();
            }

            return result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
            /*
            if( super.cancel() ) {
                var result = false;
                for (var embed : generatorsStack) {
                    if (embed.generator instanceof Cancellable<?> isCancellable) {
                        result = result || isCancellable.cancel();
                    }
                }
                return result;
            }
            return false;
            */
        }
    }

    @FunctionalInterface
    interface EmbedCompletionHandler {
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
        }

        ;
    }

    /**
     * Represents a data element in the AsyncGenerator.
     *
     * @param <E> the type of the data element
     */
    record Data<E>(
            CompletableFuture<E> future,
            AsyncGenerator.Embed<E> embed,
            Object resultValue) {

        public boolean isDone() {
            return (future == null && embed == null);
        }

        public boolean isError() {
            return future != null && future.isCompletedExceptionally();
        }

        public static <E> Data<E> of(CompletableFuture<E> future) {
            return new Data<>(requireNonNull(future, "future task cannot be null"), null, null);
        }

        public static <E> Data<E> of(E data) {
            return new Data<>(completedFuture(data), null, null);
        }

        public static <E> Data<E> composeWith(AsyncGenerator<E> generator, AsyncGenerator.EmbedCompletionHandler onCompletion) {
            return new Data<>(null, new AsyncGenerator.Embed<>(generator, onCompletion), null);
        }

        public static <E> Data<E> done() {
            return new Data<>(null, null, null);
        }

        public static <E> Data<E> done(Object resultValue) {
            return new Data<>(null, null, resultValue);
        }

        public static <E> Data<E> error(Throwable exception) {
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

    /**
     * Maps the elements of this generator to a new asynchronous generator.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped elements
     */
    default <U> AsyncGenerator<U> map(Function<E, U> mapFunction) {
        return new Mapper<>(this, mapFunction);
    }

    /**
     * Maps the elements of this generator to a new asynchronous generator, and flattens the resulting nested generators.
     *
     * @param mapFunction the function to map elements to a new asynchronous counterpart
     * @param <U>         the type of elements in the new generator
     * @return a generator with mapped and flattened elements
     */
    default <U> AsyncGenerator<U> flatMap(Function<E, CompletableFuture<U>> mapFunction) {
        return new FlatMapper<>(this, mapFunction);
    }

    private CompletableFuture<Object> forEachSync(Consumer<E> consumer) {
        final var next = next();
        if (next.isDone()) {
            return completedFuture(next.resultValue());
        }
        if (next.embed() != null) {
            return next.embed().generator.forEachSync(consumer)
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
        return CompletableFuture.supplyAsync(() -> forEachSync(consumer), executor()).join();
    }

    default <R> CompletableFuture<R> reduce(R result, BiFunction<R, E, R> reducer) {
        final var next = next();
        if (next.isDone()) {
            return completedFuture(result);
        }
        return next.future()
                .thenApply(v -> reducer.apply(result, v))
                .thenCompose(v -> reduce(result, reducer))
                ;

    }

    default <R> CompletableFuture<R> reduceAsync(R result, BiFunction<R, E, R> reducer) {
        return CompletableFuture.supplyAsync(() -> reduce(result, reducer), executor()).join();
    }

    /**
     * Converts the AsyncGenerator to a CompletableFuture.
     *
     * @return a CompletableFuture representing the completion of the AsyncGenerator
     */
    default CompletableFuture<Object> toCompletableFuture() {
        final Data<E> next = next();
        if (next.isDone()) {
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
        return new InternalIterator<E>(this);
    }


    /**
     * Returns an empty AsyncGenerator.
     *
     * @param <E> the type of elements
     * @return an empty AsyncGenerator
     */
    static <E> AsyncGenerator<E> empty() {
        return new Base<>() {
            @Override
            public Data<E> next() {
                return Data.done();
            }
        };
    }

    /**
     * Collects asynchronous elements from an iterator.
     *
     * @param <E>      the type of elements in the iterator
     * @param iterator the iterator containing elements to collect
     * @return an AsyncGenerator instance with collected elements
     */
    static <E> AsyncGenerator<E> from(Iterator<E> iterator) {
        return new Base<>() {
            @Override
            public Data<E> next() {

                if (!iterator.hasNext()) {
                    return Data.done();
                }
                return Data.of(completedFuture(iterator.next()));
            }
        };
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

        if (next == null || next.isDone()) {
            throw new IllegalStateException("no more elements into iterator");
        }

        if (!next.isError()) {
            currentFetchedData.set(delegate.next());
        }

        return next.future().join();
    }

    @Override
    public Optional<Object> resultValue() {
        if (delegate instanceof AsyncGenerator.HasResultValue withResult) {
            return withResult.resultValue();
        }
        return Optional.empty();
    }
};

class Mapper<E, U> extends AsyncGenerator.Base<U> implements AsyncGenerator.Cancellable<U>, AsyncGenerator.HasResultValue {

    protected final AsyncGenerator<E> delegate;
    final Function<E, U> mapFunction;
    private Object resultValue;

    protected Mapper(AsyncGenerator<E> delegate, Function<E, U> mapFunction) {
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
    public Optional<Object> resultValue() {
        return ofNullable(resultValue);
    }

    ;

    @Override
    public final Data<U> next() {
        final Data<E> next = (isCancelled()) ? Data.done(CANCELLED) : delegate.next();

        if (next.isDone()) {
            resultValue = next.resultValue();
            return Data.done(next.resultValue());
        }
        return Data.of(next.future().thenApply(mapFunction));
    }

    @Override
    public boolean isCancelled() {
        if (delegate instanceof Cancellable<?> isCancellable) {
            return isCancellable.isCancelled();
        }
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (delegate instanceof Cancellable<?> isCancellable) {
            return isCancellable.cancel(mayInterruptIfRunning);
        }
        return false;
    }
}

class FlatMapper<E, U> extends AsyncGenerator.Base<U> implements AsyncGenerator.Cancellable<U>, AsyncGenerator.HasResultValue {

    protected final AsyncGenerator<E> delegate;
    final Function<E, CompletableFuture<U>> mapFunction;
    private Object resultValue;

    protected FlatMapper(AsyncGenerator<E> delegate, Function<E, CompletableFuture<U>> mapFunction) {
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
    public Optional<Object> resultValue() {
        return ofNullable(resultValue);
    }

    ;

    @Override
    public final Data<U> next() {
        final Data<E> next = (isCancelled()) ? Data.done(CANCELLED) : delegate.next();

        if (next.isDone()) {
            resultValue = next.resultValue();
            return Data.done(next.resultValue());
        }
        return Data.of(next.future().thenCompose(mapFunction));
    }

    @Override
    public boolean isCancelled() {
        if (delegate instanceof Cancellable<?> isCancellable) {
            return isCancellable.isCancelled();
        }
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (delegate instanceof Cancellable<?> isCancellable) {
            return isCancellable.cancel(mayInterruptIfRunning);
        }
        return false;
    }
}



