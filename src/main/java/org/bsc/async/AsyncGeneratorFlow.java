package org.bsc.async;

import org.bsc.async.internal.BlockingQueueProcessor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.bsc.async.AsyncGenerator.*;

/**
 * Represents a queue-based asynchronous generator.
 */
public interface AsyncGeneratorFlow {

    interface Dispatcher<E> {
        void dispatchSync( Data<E> data ) throws InterruptedException;
        void dispatchAsync( Data<E> data );
    }

    interface Receiver<E> {
        Data<E> waitSync() throws InterruptedException;
        Optional<Data<E>> waitAsync();

    }

    interface Processor<E> extends Dispatcher<E>, Receiver<E> {
    }

    class EmbedProcessor<E> implements AsyncGeneratorFlow.Processor<E> {

        public record ResultValue( Object resultValue )  {

            public static ResultValue of( AsyncGenerator.Data<?> data ) {
                return new ResultValue( data.resultValue() );
            }
        }

        static <E> AsyncGeneratorFlow.Processor<E> of(AsyncGeneratorFlow.Processor<E> processor ) {
            return new EmbedProcessor<>( processor );
        }

        final AsyncGeneratorFlow.Processor<E> parent;

        protected EmbedProcessor(AsyncGeneratorFlow.Processor<E> parent) {
            this.parent = requireNonNull(parent, "parent cannot be null");
        }

        @Override
        public void dispatchSync(AsyncGenerator.Data<E> data) throws InterruptedException {
            parent.dispatchSync( data.isDone() ?
                    new AsyncGenerator.Data<>( null, null, ResultValue.of(data) ) :
                    data);
        }

        @Override
        public void dispatchAsync(AsyncGenerator.Data<E> data) {
            parent.dispatchAsync( data.isDone() ?
                    new AsyncGenerator.Data<>( null, null, ResultValue.of(data) ) :
                    data);
        }

        @Override
        public AsyncGenerator.Data<E> waitSync() throws InterruptedException {
            return parent.waitSync();
        }

        @Override
        public Optional<AsyncGenerator.Data<E>> waitAsync() {
            return parent.waitAsync();
        }
    }


    class Builder {
        private Processor<?> processor;
        private Executor executor;

        public <E> Builder processor( Processor<E> processor) {
            this.processor = processor;
            return this;
        }
        public Builder executor( Executor executor) {
            this.executor = executor;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <E> Generator<E> build( Consumer<Dispatcher<E>> emitter ) {
            if( processor == null ) {
                processor = new BlockingQueueProcessor<E>();
            }
            if( executor != null ) {
                CompletableFuture.runAsync(() -> emitter.accept((Dispatcher<E>) processor), executor );
            }
            else {
                CompletableFuture.runAsync( () -> emitter.accept((Dispatcher<E>) processor) );
            }
            return new AsyncGeneratorFlow.Generator<>( (Receiver<E>) processor);
        }
    }

    static Builder builder() {
        return new Builder();
    }

    static <E> Generator<E> create( Consumer<Dispatcher<E>> emitter ) {
        return builder().processor( new BlockingQueueProcessor<>() ).build( emitter );
    }

    /**
     * Inner class to generate asynchronous elements from the queue.
     *
     * @param <E> the type of elements in the queue
     */
    class Generator<E> extends BaseCancellable<E> implements HasResultValue {

        private volatile Thread executorThread = null;
        private volatile Data<E> endData = null;
        private final Receiver<E> receiver;

        public Generator(Receiver<E> receiver) {
            this.receiver = receiver;
        }

        private boolean isEnded() {
            return endData != null;
        }

        /**
         * Retrieves the next element from the queue asynchronously.
         *
         * @return the next element from the queue
         */
        @Override
        public Data<E> next() {
            if( isEnded() ) {
                return endData;
            }
            if(executorThread!=null) {
                endData = Data.error(new IllegalStateException("illegal concurrent next() invocation"));
                return endData;
            }
            executorThread = Thread.currentThread();
            try {
                Data<E> value = null;
                while( true ) {
                    value = receiver.waitSync();
                    if (value.isDone() ) {

                        if( value.resultValue() instanceof EmbedProcessor.ResultValue ) {
                            continue;
                        }
                        endData = value;
                    }
                    break;
                }
                return value;
            } catch (InterruptedException e) {
                endData = Data.done(CANCELLED);
                return endData;
            }
            finally {
                executorThread = null;
            }
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            if( super.cancel(mayInterruptIfRunning) ) {
                if( executorThread != null ) {
                    executorThread.interrupt();
                }
                return true;
            }
            return false;
        }

        @Override
        public Optional<Object> resultValue() {
            return ofNullable( endData ).map( Data::resultValue );
        }
    }


}
