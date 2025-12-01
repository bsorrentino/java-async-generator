# AsyncGenerator Cancellation

The `AsyncGenerator` library provides a mechanism to cancel an ongoing iteration. This document explains how to use this feature.

## IsCancellable Interface

To support cancellation, an `AsyncGenerator` must implement the `IsCancellable` interface. This interface provides the following methods:

- `cancel(boolean mayInterruptIfRunning)`: This method is used to request the cancellation of the iteration.
- `isCancelled()`: This method returns `true` if the cancellation has been requested, and `false` otherwise.

The `AsyncGenerator.WithResult` and `AsyncGenerator.WithEmbed` decorators already implement this interface.

## Iteration and Threading

The way the iteration is performed affects how cancellation works.

- **`AsyncGenerator.forEachAsync()`**: This method creates a new single-threaded executor for each generator and executes the entire iteration on that thread. This means that the iteration runs in the background, and the `forEachAsync` method returns a `CompletableFuture` that is completed when the iteration is finished.

- **`AsyncGenerator.iterator()`**: This method returns an `Iterator` that can be used to iterate over the generator's values. The iteration is executed in the same thread that calls the `next()` method on the iterator.

## Cancellation Behavior

The `cancel(boolean mayInterruptIfRunning)` method has two different behaviors depending on the value of the `mayInterruptIfRunning` parameter.

- **`cancel(false)`**: This sets an internal flag that will stop the iteration when the next value is requested. The currently executing task will not be interrupted. This is useful when you want to gracefully stop the iteration without interrupting any ongoing work.

- **`cancel(true)`**: This sets the same internal flag as `cancel(false)`, but it also attempts to interrupt the thread on which the iteration is executing. This is useful when you want to immediately stop the iteration, even if it means interrupting a long-running task.

## Checking for Interruption

When an iteration is cancelled, the `CompletableFuture` returned by `forEachAsync` will be completed exceptionally with a `CancellationException`. You can use the `exceptionally` method to handle this case.

Here is an example from `asyncGeneratorForEachCancelTest`:

```java
var futureResult = cancellableIt.forEachAsync( value -> {
        try {
            System.out.printf( "adding element: %s on thread[%s]\n", value, Thread.currentThread().getName());
            Thread.sleep( 500 );
            forEachResult.add(value);
            System.out.printf( "added element: %s\n", value);
        } catch (InterruptedException e) {
            System.err.printf("interrupted on : %s\n", value );
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    } ).exceptionally( throwable -> {
            assertInstanceOf( InterruptedException.class, throwable.getCause());
            return AsyncGenerator.Cancellable.CANCELLED;
        });

var result = futureResult.get( 5, TimeUnit.SECONDS);

assertNotNull( result );
assertEquals(AsyncGenerator.Cancellable.CANCELLED, result );
```

In this example, the `exceptionally` block catches the `CompletionException` which is thrown when the thread is interrupted, and returns the `CANCELLED` sentinel value.

## Cancellation with Embedded Generators

The `asyncEmbedGeneratorWithResultCancelTest` shows how cancellation works with embedded generators. In this case, calling `cancel(false)` on the `WithEmbed` generator will propagate the cancellation request to the embedded generators.

```java
CompletableFuture.runAsync( () -> {
    try {
        Thread.sleep(2000);
        var cancelled = it.cancel( false );
        assertTrue( cancelled );
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});
```

The iteration will stop when the next value is requested from the current generator, and the `isCancelled()` method will return `true`.
