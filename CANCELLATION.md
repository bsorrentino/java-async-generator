# AsyncGenerator Cancellation

The `AsyncGenerator` provides a mechanism to cancel an ongoing iteration. This is particularly useful for long-running asynchronous sequences.

## IsCancellable Interface

Cancellation is supported by generators that are compliant with the `IsCancellable` interface. This interface provides the core methods for cancellation:

```java
interface IsCancellable  {
    boolean isCancelled();
    boolean cancel(boolean mayInterruptIfRunning);
}
```

An `AsyncGenerator` can be made cancellable, for example, by wrapping it with `AsyncGenerator.WithResult` or by using a generator that extends `AsyncGenerator.BaseCancellable`.

## Cancellation Behavior

The `cancel(boolean mayInterruptIfRunning)` method allows for two types of cancellation:

### 1. Graceful Cancellation 

When you invoke `cancel(false)`, the generator sets an internal "cancelled" flag to `true`. The iteration will not be immediately terminated. Instead, it will stop gracefully before processing the *next* element. This ensures that the current operation completes, but no new operations are started. This is useful when you want to allow the current asynchronous task to finish its work to avoid leaving the system in an inconsistent state.

### 2. Immediate Cancellation

Invoking `cancel(true)` also sets the internal "cancelled" flag. In addition, it attempts to interrupt the underlying thread that is executing the iteration. This is a more forceful cancellation and can be useful when you need to stop a long-running or blocked operation immediately. This will typically result in an `InterruptedException` being thrown within the task's execution block.

## Threading and Iteration

The cancellation behavior is closely tied to how the `AsyncGenerator` is consumed.

### Using forEachAsync(consumer)

When you use `forEachAsync(consumer)`, the iteration is executed on a new, dedicated single-thread executor.

-   `cancel(false)` will cause the loop to terminate before the next element is processed.
-   `cancel(true)` will interrupt the dedicated thread, causing the `forEachAsync` `CompletableFuture` to complete exceptionally (often with an `InterruptedException`).

**Example from `asyncGeneratorForEachCancelTest`:**

```java
final var data = List.of( "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10" );
final AsyncGenerator<String> it = AsyncGenerator.from(data.iterator());
final var cancellableIt = new AsyncGenerator.WithResult<>(it);

CompletableFuture.runAsync( () -> {
    try {
        Thread.sleep( 2000 );
        cancellableIt.cancel(true); // Interrupt the thread
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});

var futureResult = cancellableIt.forEachAsync( value -> {
    try {
        Thread.sleep( 500 );
        forEachResult.add(value);
    } catch (InterruptedException e) {
        // The thread is interrupted here
        Thread.currentThread().interrupt();
        throw new CompletionException(e);
    }
} ).exceptionally( throwable -> {
    assertInstanceOf( InterruptedException.class, throwable.getCause());
    return AsyncGenerator.Cancellable.CANCELLED;
});
```

### Using iterator()

When you use the standard `for-each` loop with an `AsyncGenerator` (which uses the `iterator()` method), the iteration runs on the *current* thread. The `iterator()` blocks on each call to `next()` until the `CompletableFuture` for that element is resolved.

-   `cancel(false)` will cause `hasNext()` to return `false` on the next check, effectively stopping the loop.
-   `cancel(true)` will also cause `hasNext()` to return `false`. Since the iteration is running on the calling thread, interrupting it depends on how the caller manages its own thread. The `cancel(true)` call will attempt to shutdown the generator's executor, but if the main work is happening on the calling thread via the iterator, the effect of thread interruption is different than with `forEachAsync`.

## Summary

In summary, `cancel(false)` provides a non-disruptive way to signal termination, while `cancel(true)` offers a more immediate stop by leveraging thread interruption, which is most effective with `forEachAsync`.

# Conclusion

We must understand that the cancellation is a cooperative game and to make it effective we must be aware of this. Anyway the `async-generator` library provides a base implementation to make this game easier to play.

## Next Version - AbortController

n the next version we have planned tp implement an `AbortController` allowing to provide a way to interrupt asynchronous task that could spawn different threads