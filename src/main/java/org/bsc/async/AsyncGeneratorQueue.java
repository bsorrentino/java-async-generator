package org.bsc.async;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

public class AsyncGeneratorQueue<E> implements AsyncGenerator<E> {

    final static Logger logger = Logger.getLogger( AsyncGeneratorQueue.class.getName() );

    static void trace( String message, Object... args ) {
        logger.log(Level.FINE, format(message, args ) );
    }

    public static class Item<E> {
        final  AsyncGenerator.Data<E> data;
        final Throwable error;

        private Item(AsyncGenerator.Data<E> data, Throwable error) {
            this.data = data;
            this.error = error;
        }

        boolean isEnd() { return data.done;  }
        boolean isError() {
            return error != null;
        }

        static <E> Item<E> of(AsyncGenerator.Data<E> data) {
            return new Item<>(data, null);
        }

        static <E> Item<E> of(Throwable error) {
            return new Item<>(null, error);
        }

        @Override
        public String toString() {
            if( isError() ) {
                return format("Item error: %s", error.getMessage() );
            }
            if( isEnd() ) {
                return format("Item end: %s", data.data);
            }
            return String.valueOf(data.data);
        }
    }

    static final public class Builder<T> {
        private BlockingQueue<Item<T>> queue;

        private Executor executor;

        Long fetchTimeout = null;
        TimeUnit timeoutUnit  = TimeUnit.SECONDS;


        public Builder<T> queue(BlockingQueue<Item<T>> queue) {
            this.queue = queue;
            return this;
        }

        public Builder<T> executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder<T> fetchTimeout(long timeout, TimeUnit unit) {
            this.fetchTimeout = timeout;
            this.timeoutUnit = unit;
            return this;
        }
        public AsyncGeneratorQueue<T> build() {
            return new AsyncGeneratorQueue<>(queue, executor, fetchTimeout, timeoutUnit);
        }
    }

    public static <E> Builder<E> builder() {
        return new Builder<E>();
    }

    final BlockingQueue<Item<E>> queue;

    private final Executor executor;

    private final Long fetchTimeout;
    private final TimeUnit timeoutUnit;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private AsyncGeneratorQueue(BlockingQueue<Item<E>> queue, Executor executor, Long fetchTimeout, TimeUnit timeoutUnit) {
        if( queue == null ) {
            queue = new SynchronousQueue<>();
        }
        if( executor == null ) {
            executor = Runnable::run;
        }

        if( fetchTimeout != null && timeoutUnit == null   ) {
            throw new IllegalStateException("timeout unit is not set");
        }

        this.queue = queue;
        this.executor = executor;
        this.fetchTimeout = fetchTimeout;
        this.timeoutUnit = timeoutUnit;
    }

    private Item<E> waitForNext() throws InterruptedException, TimeoutException {
        if( fetchTimeout == null) {
            return queue.take();
        }
        Item<E> result = queue.poll(fetchTimeout, timeoutUnit);
        if( result == null ) {
            throw new TimeoutException( format("queue exceed the poll timeout %d %s", fetchTimeout, timeoutUnit) );
        }
        return result;
    }
    @Override
    public final CompletableFuture<AsyncGenerator.Data<E>> next() {
        // GUARD: call next after close generator
        if( isClosed.get() && queue.peek()==null ) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync( () -> {
            try {
                Item<E> result = waitForNext();
                if (result.isError()) {
                    trace("next error: [%s]", result.error.getMessage());
                    throw new RuntimeException(result.error);
                }
                if( result.isEnd() ) {
                    trace("end [%s]", result.data.data);
                    return result.data;
                }
                trace("next [%s]", result.data.data);
                return result.data;
            } catch (InterruptedException e ) {
                throw new RuntimeException(e);
            }
            catch( TimeoutException e ) {
                if( isClosed.get() ) {
                    return null;
                }
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public final boolean close() {
        trace("close - isClosed:[%b]", isClosed.get() );
        if( isClosed.getAndSet(true) ) {
            return false;
        }
        return queue.add(Item.of( Data.done()));
    }

    public final boolean closeExceptionally( Throwable ex ) {
        trace("closeExceptionally - isClosed:[%b]", isClosed.get());
        if( isClosed.getAndSet(true)) {
            throw new IllegalStateException("generator is closed");
        }
        return queue.add(Item.of( ex ));


    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted queue, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * @param eItem the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException    if the element cannot be added at this
     *                                  time due to capacity restrictions
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public boolean add(E eItem) {
        if( isClosed.get()) {
            throw new IllegalStateException("generator is closed");
        }
        return queue.add( Item.of( Data.of(eItem) ));
    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted queue, this method is
     * generally preferable to {@link #add}, which can fail to insert an
     * element only by throwing an exception.
     *
     * @param eItem the element to add
     * @return {@code true} if the element was added to this queue, else
     * {@code false}
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public boolean offer(E eItem) {
        if( isClosed.get()) {
            throw new IllegalStateException("generator is closed");
        }
        return queue.offer( Item.of( Data.of(eItem)));
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param eItem the element to add
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public void put(E eItem) throws InterruptedException {
        if( isClosed.get()) {
            throw new IllegalStateException("generator is closed");
        }
        queue.put(Item.of( Data.of(eItem)));
    }

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param eItem   the element to add
     * @param timeout how long to wait before giving up, in units of
     *                {@code unit}
     * @param unit    a {@code TimeUnit} determining how to interpret the
     *                {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     * the specified waiting time elapses before space is available
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */
    public boolean offer(E eItem, long timeout, TimeUnit unit) throws InterruptedException {
        if( isClosed.get()) {
            throw new IllegalStateException("generator is closed");
        }
        return queue.offer(Item.of( Data.of(eItem)), timeout, unit);
    }

}
