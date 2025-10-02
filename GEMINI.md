as my java architect, concerning this project named java-async-generator i need that you create documentation about the new feature allowing to cancel an iteration during its execution. the result must be put in `CANCELLATION.md`

The implementation is in the source file @src/main/java/org/bsc/async/AsyncGenerator.java. 

To understand how to use use the Unit Test in the file @src/test/java/org/bsc/async/AsyncGeneratorTest.java and take a look to method `asyncGeneratorForEachCancelTest()` and `asyncEmbedGeneratorWithResultCancelTest()`

Explain in the clear technical way that:

* Cacellation is achieved invoking `cancel(boolean mayInterruptIfRunning)` method on an `AsyncGenerator` that is compliant with `IsCancellable` interface
* Invoking `AsyncGenerator.forEachAsync()` will be create a new single thread where will be executed entire iteration over generator values 
* Invoking `AsyncGenerator.iterator()` will not create any thread and iteration will be executed in the current thread
* Invoking `IsCancellable.cancel(false)` set an internal state that will stop iteration when a next value is required
* Invoking `IsCancellable.cancel(true)` set an internal state and try to interrupt the thread on which the iteration is executing
* how to check if iteration has been interrupted
