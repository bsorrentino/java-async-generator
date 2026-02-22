# Changelog



<!-- "name: v4.2.0" is a release tag -->

## [v4.2.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v4.2.0) (2026-02-22)



### Documentation

 -  bump to next version 4.2.0 ([04af96be1ead009](https://github.com/bsorrentino/java-async-generator/commit/04af96be1ead0098429e98baf003ad05c20f7f3f))

 -  bump to version  4.1.0 ([28d614079852036](https://github.com/bsorrentino/java-async-generator/commit/28d6140798520366d6a7742c2acd8282db4204c5))

 -  update changeme ([5e7d727f6dd3733](https://github.com/bsorrentino/java-async-generator/commit/5e7d727f6dd3733e6d7e961148a05a2663201389))


### Refactor

 -  **AsyncGenerator**  Refactor async methods to use thenCompose instead of join ([b0baab9c2c3a7a9](https://github.com/bsorrentino/java-async-generator/commit/b0baab9c2c3a7a9a2a82ff2e7ce6f8f4b4a4f220))
    > Replace .join() with .thenCompose() for non-blocking behavior and add new toCompletableFutureAsync() method


### ALM 

 -  bump to next version 4.2.0 ([5c333e54bac9af7](https://github.com/bsorrentino/java-async-generator/commit/5c333e54bac9af78d5d8563abe8bacfd764e85b3))
   
 -  move to nex dev version 4.2-SNAPSHOT ([3c643b510e29adc](https://github.com/bsorrentino/java-async-generator/commit/3c643b510e29adc10b0c7ffd317cd79d4ee6bb24))
   

### Test 

 -  update test to verify the AsyncGenerator behaviour ([c3bd51e7ef6de8f](https://github.com/bsorrentino/java-async-generator/commit/c3bd51e7ef6de8fdde8a850b48df2aee71b6294e))
   
 -  **Task**  add  sync task handling ([fea665b8a74d10e](https://github.com/bsorrentino/java-async-generator/commit/fea665b8a74d10ecc66d43e507cd0e8d90bd5660))
   





<!-- "name: v4.1.0" is a release tag -->

## [v4.1.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v4.1.0) (2026-02-16)


### Bug Fixes

 -  avoid lambda capturing 'this' by using static ID generator directly ([8c45b2384bf526a](https://github.com/bsorrentino/java-async-generator/commit/8c45b2384bf526ae47b5be00840416eca0f8cc2f))

 -  resolve ExecutorService memory leak on JDK 21 ([09ab028d26ee567](https://github.com/bsorrentino/java-async-generator/commit/09ab028d26ee56775d44daac2ea631daf41ec659))


### Documentation

 -  update changeme ([4c1c8d58af76be8](https://github.com/bsorrentino/java-async-generator/commit/4c1c8d58af76be89bf76c5a306f04b546f0d0e05))



### ALM 

 -  bump to version  4.1.0 ([a4e532162f19082](https://github.com/bsorrentino/java-async-generator/commit/a4e532162f190827e88043e28209ee6d0b5e08c7))
   






<!-- "name: v4.0.0" is a release tag -->

## [v4.0.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v4.0.0) (2026-01-15)

### Features

 *  add toCompletableFuture async version ([f3707f1e8807df3](https://github.com/bsorrentino/java-async-generator/commit/f3707f1e8807df317da4301841b931ed2e0e8547))
   


### Documentation

 -  bump to version 4.0.0 ([ff972ba0304f230](https://github.com/bsorrentino/java-async-generator/commit/ff972ba0304f230e20fa6212ed85368e6c037d4c))

 -  update changeme ([1883fa772348f7b](https://github.com/bsorrentino/java-async-generator/commit/1883fa772348f7bf332812601024083b6c6ba623))


### Refactor

 -  **AsyncGenerator**  Refactor Embed to record and fix syntax ([26c473efc9a8e75](https://github.com/bsorrentino/java-async-generator/commit/26c473efc9a8e75b08bc2ea7cc0f4b06821713b5))
   
 -  **AsyncGeneratorQueue**  Refactor class to interface ([ee93ff2677a29ba](https://github.com/bsorrentino/java-async-generator/commit/ee93ff2677a29bae37c84ac9ee114ea415be8499))
    > Converted AsyncGeneratorQueue class to interface and adjusted static method signatures

 -  shift left the join() in toCompletableFuture async version ([05dd58ba274f14f](https://github.com/bsorrentino/java-async-generator/commit/05dd58ba274f14fa52fb926cb00c3e35bbaabc99))
   

### ALM 

 -  bump to version 4.0.0 ([bc57a84660a6317](https://github.com/bsorrentino/java-async-generator/commit/bc57a84660a6317d324dbc3ad8adc9f82c38ae59))
   
 -  bump to next dev version ([b774d813de93d8f](https://github.com/bsorrentino/java-async-generator/commit/b774d813de93d8fa6a45f46e5a7e354c9bc3001a))
   






<!-- "name: v4.0.0-beta2" is a release tag -->

## [v4.0.0-beta2](https://github.com/bsorrentino/java-async-generator/releases/tag/v4.0.0-beta2) (2025-10-02)



### Documentation

 -  bump to 4.0.0-beta2 version ([2f8860fb77fc1b4](https://github.com/bsorrentino/java-async-generator/commit/2f8860fb77fc1b45521f1904eb32b8cc9f2de7d2))

 -  update cancellation doc ([7e100f9432b5726](https://github.com/bsorrentino/java-async-generator/commit/7e100f9432b572627b55eb074adee21d18ab5585))

 -  update cancellation doc ([07c9e389b439c96](https://github.com/bsorrentino/java-async-generator/commit/07c9e389b439c965043884c8e490e336fac2837b))

 -  update GEMINI cli default prompt ([bc0aa5bfeebba13](https://github.com/bsorrentino/java-async-generator/commit/bc0aa5bfeebba131ddecbe9cdb166ae7eb3f11f7))

 -  refine cancellation guide ([fc689106ec11025](https://github.com/bsorrentino/java-async-generator/commit/fc689106ec110250e745c3520294bfa7408fe0d8))
     > work on #2

 -  update changeme ([f9fa28ac55169c1](https://github.com/bsorrentino/java-async-generator/commit/f9fa28ac55169c105484f26d385de6f7a1ca1d91))


### Refactor

 -  return CANCELLED result if cancellation is detected in next() method ([4f5c479031a3169](https://github.com/bsorrentino/java-async-generator/commit/4f5c479031a31691cd6f22f9a1255b23f2e97db5))
    > work on #2

 -  **AsyncGenerator**  Refactored cancellation logic across AsyncGenerator subclasses ([1046e25b0004110](https://github.com/bsorrentino/java-async-generator/commit/1046e25b00041100da297afd21784413882eae77))
    > - handle cancellation regardless that delegate is cancellable
 > work on #2


### ALM 

 -  bump to 4.0.0-beta2 version ([720ef0ba54250e3](https://github.com/bsorrentino/java-async-generator/commit/720ef0ba54250e3fe5a07291bf73a9843b1031f9))
   
 -  **action**  update deploy snapshot github action ([0d324a536bddd74](https://github.com/bsorrentino/java-async-generator/commit/0d324a536bddd741f7e1e913ac990bdbe8a2dacf))
   
 -  bump to 4.0-SNAPSHOT release ([756ba4fb9f4461f](https://github.com/bsorrentino/java-async-generator/commit/756ba4fb9f4461f43974e3b732bc060deff61f00))
   
 -  update deploy script ([4cc8accbbda3812](https://github.com/bsorrentino/java-async-generator/commit/4cc8accbbda3812517defcd5ed880b6e29632a5e))
   

### Test 

 -  **AsyncGenerator**  Refactored cancellation logic across AsyncGenerator subclasses ([23b9a57ff806dcf](https://github.com/bsorrentino/java-async-generator/commit/23b9a57ff806dcfafae764734a31117b131ab1f6))
    > - handle cancellation regardless that delegate is cancellable
 > work on [#2](https://github.com/bsorrentino/java-async-generator/issues/2)






<!-- "name: v4.0.0-beta1" is a release tag -->

## [v4.0.0-beta1](https://github.com/bsorrentino/java-async-generator/releases/tag/v4.0.0-beta1) (2025-10-01)

### Features

 *  use service.shutdownNow() to force thread interruption ([4942910ec9fafcd](https://github.com/bsorrentino/java-async-generator/commit/4942910ec9fafcd9023b1ee990a21b6911aa11d1))
   
 *  Add AbstractCancellableAsyncGenerator class ([2512f981de6ba91](https://github.com/bsorrentino/java-async-generator/commit/2512f981de6ba91d59fef936f1a5dba16ab7f4f4))
     > - new abstract class that implements AsyncGenerator.Cancellable interface, providing functionality to keep cancellation state .
     > work on #2
   
 *  **AsyncGenerator**  Update AsyncGenerator interface for better cancellation support ([731abd0a76e3d2e](https://github.com/bsorrentino/java-async-generator/commit/731abd0a76e3d2ede66d11711910808ba84ebf71))
     > - Added Cancellable
     > - Updated WithResult and WithEmbed classes to implement these new interfaces for managing  cancellation request.
     > work on #2
   

### Bug Fixes

 -  **AsyncGenerator**  Ensure Data.done() includes resultValue ([c5f59dea24202cc](https://github.com/bsorrentino/java-async-generator/commit/c5f59dea24202cc88ce9e1294f24a463b059a1d3))
     > Modify return statements in internal class Mapper and FlatMapper to pass resultValue w.
     > work on #2


### Documentation

 -  bump to new version 4.0.0-beta1 ([76daba668e13893](https://github.com/bsorrentino/java-async-generator/commit/76daba668e1389343ba46f5e523aa1b46fa48776))

 -  update readme ([42d058811c1b28a](https://github.com/bsorrentino/java-async-generator/commit/42d058811c1b28af06c048b269b24199ee7573b9))

 -  **ai**  prompt used in gemini cli ([fc84ce5aaec8c3f](https://github.com/bsorrentino/java-async-generator/commit/fc84ce5aaec8c3fed7ef1a2ca607809383d0583c))

 -  add cancellation document ([668092008616bf4](https://github.com/bsorrentino/java-async-generator/commit/668092008616bf42cbad28e648d70dc20c303bd1))

 -  update changeme ([bb10611598b04f2](https://github.com/bsorrentino/java-async-generator/commit/bb10611598b04f2b901ed9c611c2588c3969dab9))


### Refactor

 -  **AsyncGenerator**  Rename abstract class BaseCancellable to use Cancellable interface instead of IsCancellable ([9b87691850bf9bd](https://github.com/bsorrentino/java-async-generator/commit/9b87691850bf9bd66b411fcf611d7adf1711986b))
    > work on #2

 -  **AsyncGenerator**  extract feature driven IsCancelled interface ([805925b193860e8](https://github.com/bsorrentino/java-async-generator/commit/805925b193860e8c32a919566e8a143b25dd2e41))
    > work on #2

 -  **AsyncGenerator**  make reduce public ([06099f66d0d8a14](https://github.com/bsorrentino/java-async-generator/commit/06099f66d0d8a14740e4c83916933d422145e453))
    > work on #2

 -  **AsyncGenerator**  rename reduceSync to reduce and update references ([56d4e15242918da](https://github.com/bsorrentino/java-async-generator/commit/56d4e15242918da2d03e2467fd57cfb98dfbead4))
    > work on #2

 -  Merge AsyncGenerator with AsyncGeneratorBase ([13a9c8a4b8d6617](https://github.com/bsorrentino/java-async-generator/commit/13a9c8a4b8d6617a6fa23b529048039458d8283d))
    > work on #2

 -  **reactive**  update with new Cancellable model ([c2c5437928bf1cd](https://github.com/bsorrentino/java-async-generator/commit/c2c5437928bf1cd76337fcc35bb0941487be0460))
   
 -  **AsyncGenerator**  Refactor AsyncGenerator class with ExecutorService support to force execution on a single controlled thread ([e9439a1fc9f8ea7](https://github.com/bsorrentino/java-async-generator/commit/e9439a1fc9f8ea7bf7e3aacda89abed3c662e1f5))
    > work on #2

 -  remove unused AbstractCancellableAsyncGenerator ([57c3e0251b4d54f](https://github.com/bsorrentino/java-async-generator/commit/57c3e0251b4d54ffed086e1f6e4608360efd9750))
    > work on #2

 -  make FlowGenerator compliant with AsyncGenerator.Cancellable ([2f07f1b4a83f24f](https://github.com/bsorrentino/java-async-generator/commit/2f07f1b4a83f24fa212dc479fbe6865caff7cdf1))
    > work on #2

 -  **reactive**  Refactor GeneratorSubscriber to manage cancellation ([4f6f8b3f468676f](https://github.com/bsorrentino/java-async-generator/commit/4f6f8b3f468676f6966baf213f0e03dac176cac9))
    > - adding a private subscription field to hold the Flow.Subscription on which we will invoke cancel() during cancellation request process
 > work on #2

 -  **AsyncGeneratorQueue**  Refactor AsyncGeneratorQueue to manage cancellation ([0e84e4c5a5afaf6](https://github.com/bsorrentino/java-async-generator/commit/0e84e4c5a5afaf6d21eec145d948350090f0609a))
    > - keep track of execution thread and perform its interruption on cancellation request
 > work on #2

 -  rename AsyncGeneratorOperators to AsyncGeneratorBase ([53efcfba852cf51](https://github.com/bsorrentino/java-async-generator/commit/53efcfba852cf519655d75351af2b73474d877c7))
    > work on #2


### ALM 

 -  bump to new version 4.0.0-beta1 ([12b2a80a091728c](https://github.com/bsorrentino/java-async-generator/commit/12b2a80a091728c983cbcaec2082a3085a8979e0))
   
 -  bump to version 4.0-SNAPSHOT ([c81d50f7879c735](https://github.com/bsorrentino/java-async-generator/commit/c81d50f7879c7354f6973b8133e548968edea30f))
   
 -  bump to 3.2-SNAPSHOT version ([e20d846ba5d9c3e](https://github.com/bsorrentino/java-async-generator/commit/e20d846ba5d9c3eacba363663901e1d2ce8cdb11))
   
 -  **settings-template.xml**  update xml namespaces ([df2f07ac67f362f](https://github.com/bsorrentino/java-async-generator/commit/df2f07ac67f362f87bfe8cbe8d326afa344d99de))
   

### Test 

 -  update unit test add cancellation tests ([c7e9f28e31b9387](https://github.com/bsorrentino/java-async-generator/commit/c7e9f28e31b938797854c0f88a98edf729daa895))
   
 -  add cancellation tests ([cb6b15653a2b860](https://github.com/bsorrentino/java-async-generator/commit/cb6b15653a2b86035b883c025475be942f4aa0c6))
   





<!-- "name: v3.2.3" is a release tag -->

## [v3.2.3](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.2.3) (2025-09-19)

### Features

 *  **GeneratorPublisher**  Update cancellation method implementation ([324a10e2a85f62e](https://github.com/bsorrentino/java-async-generator/commit/324a10e2a85f62eb8820e26d5407148f449ff8b8))
     > Replace throw statement with delegate.cancel() call to handle cancellation request on subscription.
     > work on #2
   
 *  add cancel method to allow its extensions to implement a cancellation strategy ([dec058e58d25e23](https://github.com/bsorrentino/java-async-generator/commit/dec058e58d25e230a489210b0d0c0be8c22de061))
     > work on #2
   


### Documentation

 -  bump to version 3.2.3 ([11cf718cf09df62](https://github.com/bsorrentino/java-async-generator/commit/11cf718cf09df62ff24eec9901761a633517d598))

 -  update changeme ([363a524c24fc79f](https://github.com/bsorrentino/java-async-generator/commit/363a524c24fc79fec0a024e1777a978329e69378))

 -  update changeme ([26c0fb3fd9228ff](https://github.com/bsorrentino/java-async-generator/commit/26c0fb3fd9228ffe0a0feea160e60ae4cecb27bf))


### Refactor

 -  default implementation of cancel() method raises UnsupportedOperationException ([a46cf8a01937f7c](https://github.com/bsorrentino/java-async-generator/commit/a46cf8a01937f7c128a25480e912bfdac059d85a))
    > work on #2

 -  **AsyncGeneratorQueue**  Removed deprecated method of(Q, Consumer, Executor) from AsyncGeneratorQueue ([6b5e7eec9f42d36](https://github.com/bsorrentino/java-async-generator/commit/6b5e7eec9f42d362b73d0fa1aafbf91d86c8c039))
   

### ALM 

 -  bump to version 3.2.3 ([acc241506cdf609](https://github.com/bsorrentino/java-async-generator/commit/acc241506cdf6098b2eb32d782963e773164dfb2))
   






<!-- "name: v3.2.2" is a release tag -->

## [v3.2.2](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.2.2) (2025-07-10)

### Features

 *  improve support for retrieve generator return value ([df3f83cf75f3d61](https://github.com/bsorrentino/java-async-generator/commit/df3f83cf75f3d61b40494dc862c00c6e92437839))
     > - add support of return value to iterator
     > - add utility methods for query return value
   


### Documentation

 -  update changeme ([e8846913e3d1b49](https://github.com/bsorrentino/java-async-generator/commit/e8846913e3d1b49527f6d2a33d51d35dae5c2b91))


### Refactor

 -  **deploy**  refactor: move to sonatype-central deployment repo ([f7365d58f4e75e2](https://github.com/bsorrentino/java-async-generator/commit/f7365d58f4e75e2bbbefe841c2ef3298247813b6))
   
 -  move to sonatype-central deployment repo ([c43cac491b9fa80](https://github.com/bsorrentino/java-async-generator/commit/c43cac491b9fa801933a209ecfb0200c5d6c34e6))
   

### ALM 

 -  bump to version 3.2.2 ([25d0d2d4f9eb641](https://github.com/bsorrentino/java-async-generator/commit/25d0d2d4f9eb6419a86c40dce64a8009251fea38))
   






<!-- "name: v3.2.1" is a release tag -->

## [v3.2.1](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.2.1) (2025-06-22)

### Features

 *  add method to create async generator with a default executor ([6a56c5a89d3c8d5](https://github.com/bsorrentino/java-async-generator/commit/6a56c5a89d3c8d52833244d39939f86c1bcfdec3))
     > - Added &#x60;async()&#x60; method as a default implementation that uses the common &#x60;ForkJoinPool&#x60;.
   


### Documentation

 -  update README.md ([c0edb346316dc82](https://github.com/bsorrentino/java-async-generator/commit/c0edb346316dc82e63e49d048a7545d56def9213))

 -  update changeme ([623e0cda5d251ad](https://github.com/bsorrentino/java-async-generator/commit/623e0cda5d251adb51a55cdfb0346af523c526d9))


### Refactor

 -  replace ReentrantReadWriteLock with AtomicReference ([49ae7dd5cbed212](https://github.com/bsorrentino/java-async-generator/commit/49ae7dd5cbed212aec341592783e84803013abc7))
    > - Introduced &#x60;AtomicReference&#x60; to manage the current fetched data, simplifying the locking mechanism and improving performance.
 > - Removed unnecessary read and write locks, reducing lock contention and enhancing concurrency.
 > resolve #3


### ALM 

 -  bump to version 3.2.1 ([007965d9fd82b5b](https://github.com/bsorrentino/java-async-generator/commit/007965d9fd82b5b32b7c0776067aaae2c1ce4c1b))
   
 -  update JDK version to Java 17 in deploy-snapshot.yaml action ([539dd9fa2e6799e](https://github.com/bsorrentino/java-async-generator/commit/539dd9fa2e6799e888f9e56176eecbf9b5425753))
   
 -  move to 3.2-SNAPSHOT ([3af1271300a9e96](https://github.com/bsorrentino/java-async-generator/commit/3af1271300a9e960eda3ba134c3daea28d6984ce))
   






<!-- "name: v3.2.0" is a release tag -->

## [v3.2.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.2.0) (2025-05-12)

### Features

 *  **GeneratorSubscriber**  add optional mapping for generator's result ([c123e0e307052bf](https://github.com/bsorrentino/java-async-generator/commit/c123e0e307052bf19fb4b4f274e3102b39ac3a13))
     > - Updated the &#x60;onComplete&#x60; method to use the &#x60;mapResult&#x60; function when generating the completion data.
   
 *  **FlowGenerator**  add parameter to map result from publisher ([e5d7f78165d8089](https://github.com/bsorrentino/java-async-generator/commit/e5d7f78165d8089059515b7cba2a556b29746c54))
   
 *  **AsyncGenerator**  add isError() method to handle exceptions in asynchronous data ([1cf6811547d0296](https://github.com/bsorrentino/java-async-generator/commit/1cf6811547d02966ff8d35f121c21c1fa2965151))
     > - remove deprecated methods
   


### Documentation

 -  update changeme ([f8d0b99fdac84d9](https://github.com/bsorrentino/java-async-generator/commit/f8d0b99fdac84d963a741f6039dc6c693035e265))

 -  update changeme ([9de329999bbcd8c](https://github.com/bsorrentino/java-async-generator/commit/9de329999bbcd8cdfcf265aaf6b85b93e2f37719))


### Refactor

 -  update changelog template ([0acddcf388ee5bb](https://github.com/bsorrentino/java-async-generator/commit/0acddcf388ee5bb858105c8bfccf94c350d328d9))
   

### ALM 

 -  bump to 3.2.0 version ([c2cb40e451208a0](https://github.com/bsorrentino/java-async-generator/commit/c2cb40e451208a0eb3dd8e0eaba5af1030e2e709))
   
 -  bump to SNAPSHOT ([659e9eecad7d23c](https://github.com/bsorrentino/java-async-generator/commit/659e9eecad7d23c7a8925e52a3e72318a47f2986))
   






<!-- "name: v3.1.0" is a release tag -->

## [v3.1.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.1.0) (2025-04-16)

### Features

 *  **AsyncGenerator.java**  add interface HasResultValue  to support result value retrieval ([fee0822f6cbacd3](https://github.com/bsorrentino/java-async-generator/commit/fee0822f6cbacd32bcd7ae080e28a0c466ba7676))
   
 *  **tests**  Added unit test AsyncGeneratorAsyncTest for asynchronous generator processing ([8f15f0bbd5ae681](https://github.com/bsorrentino/java-async-generator/commit/8f15f0bbd5ae681cc661e9f988f02ee802bdfd8c))
     > - Removed unused import and disabled the &#x60;asyncGenTest&#x60; method
   

### Bug Fixes

 -  configure Maven Javadoc Plugin to exclude module-info.java ([d9adb3d9fcc273e](https://github.com/bsorrentino/java-async-generator/commit/d9adb3d9fcc273ec939a920f42dd33a6bae287a1))


### Documentation

 -  update javadoc ([78f72b0555519ac](https://github.com/bsorrentino/java-async-generator/commit/78f72b0555519ac5ea738499886476a7e3c2c8e4))

 -  update changeme ([3b0314aa86dfce3](https://github.com/bsorrentino/java-async-generator/commit/3b0314aa86dfce3190d8ac3377de8217356a117b))


### Refactor

 -  **AsyncGeneratorOperators.java**  update async usage ([71aab83bb26c04a](https://github.com/bsorrentino/java-async-generator/commit/71aab83bb26c04a87b6285bd6f730455f61e7529))
    > - use async operator thenApplyAsync
 > - Removed redundant and unnecessary nested &#x60;forEachAsyncNested&#x60; and &#x60;collectAsyncNested&#x60; methods.

 -  **AsyncGenerator.java**  update deprecation annotations to specify removal ([2f143364b93650e](https://github.com/bsorrentino/java-async-generator/commit/2f143364b93650e2e8210569cd6187b4ba809e1b))
    > - Updated &#x60;@Deprecated&#x60; annotations to include &#x60;forRemoval &#x3D; true&#x60; in &#x60;collectAsync&#x60; methods.


### ALM 

 -  bump to 3.1.0 version ([edc31a64edbc051](https://github.com/bsorrentino/java-async-generator/commit/edc31a64edbc051103ecad17fbab5b037aed0bd7))
   
 -  bump to new SNAPSHOT ([4ccd38e1441a85d](https://github.com/bsorrentino/java-async-generator/commit/4ccd38e1441a85d4012d9daf24afd1e17dbac80a))
   






<!-- "name: v3.0.0" is a release tag -->

## [v3.0.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v3.0.0) (2025-01-21)

### Features

 *  **reactive**  add reactive stream integration ([9bf82ed82a55f4e](https://github.com/bsorrentino/java-async-generator/commit/9bf82ed82a55f4edafd0da75d1b4be6ee37907d2))
     > - AsyncGenerator fromPublisher( Flow.Publisher )
     > - Flow.Publisher toPublisher( AsyncGenerator )
     > resolve #1
   

### Bug Fixes

 -  **AsyncGenerator**  add UnmodifiableDeque import ([47d4b323f0f0306](https://github.com/bsorrentino/java-async-generator/commit/47d4b323f0f0306425f7464c925bbb239433df2e))


### Documentation

 -  update readme ([eaa1967766673c3](https://github.com/bsorrentino/java-async-generator/commit/eaa1967766673c3f226e956af199fe9d510f7fa4))

 -  add javadoc ([86841005e8dadf8](https://github.com/bsorrentino/java-async-generator/commit/86841005e8dadf840c94410f3b65d909aa767082))
     > work on #1

 -  update javadocs ([686473afb186e53](https://github.com/bsorrentino/java-async-generator/commit/686473afb186e53c2d05868809f7875d6a3e817e))

 -  update changeme ([2de8da8952074d6](https://github.com/bsorrentino/java-async-generator/commit/2de8da8952074d6f050e00f6eb815590aa3eb4c8))


### Refactor

 -  **AsyncGeneratorOperators.java**  improve asynchronous iteration and collection ([9dd666d47a19627](https://github.com/bsorrentino/java-async-generator/commit/9dd666d47a19627747a310326e7c37d763073031))
    > This commit refactors the &#x60;AsyncGeneratorOperators&#x60; interface to improve the performance of asynchronous iteration and collection operations. The changes include:
 > 1. Introducing a private method &#x60;forEachAsyncNested&#x60; to handle nested asynchronous iterations without spawning new threads.
 > 2. Using &#x60;supplyAsync&#x60; within the &#x60;forEachAsync&#x60; method to ensure that each iteration can be performed concurrently, enhancing parallel processing capabilities.
 > 3. Similarly, refactoring the &#x60;collectAsync&#x60; and &#x60;collectAsyncNested&#x60; methods to collect elements asynchronously efficiently.

 -  **FlowGenerator**  rename package and class ([43958c928c2ae68](https://github.com/bsorrentino/java-async-generator/commit/43958c928c2ae680f1f73b87529a4ef535eb25dc))
    > Renamed &#x27;FluxGenerator&#x27; to &#x27;FlowGenerator&#x27; and updated imports accordingly. This change improves readability and consistency within the project.

 -  **maven-javadoc-plugin**  update version to 3.11.1 ([ef818c6b26b81fe](https://github.com/bsorrentino/java-async-generator/commit/ef818c6b26b81fe66a2cb416117e60296722e7ff))
   

### ALM 

 -  bump to version 3.0.0 ([effd36c53f23915](https://github.com/bsorrentino/java-async-generator/commit/effd36c53f239157646531f6db8c916fe68489e9))
   
 -  move private classes in internal package ([bace2d332d2ea34](https://github.com/bsorrentino/java-async-generator/commit/bace2d332d2ea34857cd18a6b4d886de7e95f52a))
   
 -  add module-info.java ([0bdfa9f7b8ffba6](https://github.com/bsorrentino/java-async-generator/commit/0bdfa9f7b8ffba6854c6de8acdb2b0c05d7f2f60))
   
 -  break jdk8 compatibility move to Jdk17 ([6fa124b34410624](https://github.com/bsorrentino/java-async-generator/commit/6fa124b34410624f17a8061eaa857d441e761345))
   

### Test 

 -  add test for reactive stream integration ([09adc3cd805d03d](https://github.com/bsorrentino/java-async-generator/commit/09adc3cd805d03d2923eb54f2bf0cf83a0b6faca))
   





<!-- "name: v2.3.0" is a release tag -->

## [v2.3.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v2.3.0) (2024-12-02)

### Features

 *  add map and filter operators ([39ebb0396323aaa](https://github.com/bsorrentino/java-async-generator/commit/39ebb0396323aaa89018e84d0b8a3695c2add7e3))
     > Add new AsyncGeneratorOperators interface
     > Add async(executor) method to easier manage a provided custom executor
   

### Bug Fixes

 -  **pom-jdk8.xml**  update version to SNAPSHOT for async-generator-jdk8 ([1c27cf97df06971](https://github.com/bsorrentino/java-async-generator/commit/1c27cf97df069715fdfab277405800843bfb1b54))


### Documentation

 -  update comment ([99681d9380035b5](https://github.com/bsorrentino/java-async-generator/commit/99681d9380035b57e38ed109189dad56f3cea8ef))

 -  update changeme ([5ffc6b7bb18bf4d](https://github.com/bsorrentino/java-async-generator/commit/5ffc6b7bb18bf4d0c33d7639897b934e191007d8))


### Refactor

 -  update async-generator version to SNAPSHOT ([5ffcf2b873f2963](https://github.com/bsorrentino/java-async-generator/commit/5ffcf2b873f2963d50aab7beb9e33ebc09a9fb01))
   

### ALM 

 -  move to next version ([478001f5e078377](https://github.com/bsorrentino/java-async-generator/commit/478001f5e07837754c6d498e91bbf1da7aaa1ede))
   
 -  update actions ([f3d30d3112e0f1a](https://github.com/bsorrentino/java-async-generator/commit/f3d30d3112e0f1a395df85b2bf92f08da3068040))
   






<!-- "name: v2.2.0" is a release tag -->

## [v2.2.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v2.2.0) (2024-11-10)

### Features

 *  add support for embed generator ([6efebe3b49b1bbd](https://github.com/bsorrentino/java-async-generator/commit/6efebe3b49b1bbd4a68870cd0b2a705e24084b2e))
     > Currently not supported the recursive embed generators (ie embed of embed)
   
 *  add support for nested generator ([c924c8e70268f9d](https://github.com/bsorrentino/java-async-generator/commit/c924c8e70268f9d67fd9a8fd1348b0744f2bcdc1))
     > - provide a &#x27;resultValue&#x27; on done.
   


### Documentation

 -  update changeme ([7df0bc31be1eb25](https://github.com/bsorrentino/java-async-generator/commit/7df0bc31be1eb250dc188149501c277c5b60b22a))


### Refactor

 -  replace data.done with data.isDone() ([5dffc8ef83e286f](https://github.com/bsorrentino/java-async-generator/commit/5dffc8ef83e286fd46cff2d352ca36b8d7601d8a))
   

### ALM 

 -  bump to new version ([323a1944f53d3be](https://github.com/bsorrentino/java-async-generator/commit/323a1944f53d3becb56b6cd6cd0d0575742ad686))
   
 -  bump to next SNAPSHOT ([c080a578df9e515](https://github.com/bsorrentino/java-async-generator/commit/c080a578df9e515eed5e27e010cc772c09f41361))
   


### Continuous Integration

 -  add script for set project version ([654e3b5f8998ba8](https://github.com/bsorrentino/java-async-generator/commit/654e3b5f8998ba81446818017c8310917098531b))
   




<!-- "name: v2.1.0" is a release tag -->

## [v2.1.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v2.1.0) (2024-11-05)

### Features

 *  change visibility ([5d7b5b5a4b096e5](https://github.com/bsorrentino/java-async-generator/commit/5d7b5b5a4b096e56c60b138cb10a39e3f0489a9d))
     > publish AsyncGenerator.Data.isDone()
     > publish AsyncGeneratorQueue.Generator
   


### Documentation

 -  update readme ([980d2305fbe011e](https://github.com/bsorrentino/java-async-generator/commit/980d2305fbe011ed1c8c6de48244c06389bb464f))

 -  update changelog ([3c0a8344c30b476](https://github.com/bsorrentino/java-async-generator/commit/3c0a8344c30b47655eb39b2fa68d6b6ce4d8c905))



### ALM 

 -  bump to next version ([c91e5c5d656c693](https://github.com/bsorrentino/java-async-generator/commit/c91e5c5d656c693fac41349ae01daaccdcad983d))
   
 -  move to next development version ([452b2439f7c5a3f](https://github.com/bsorrentino/java-async-generator/commit/452b2439f7c5a3f40304a276ceb5442f31a59699))
   


### Continuous Integration

 -  add script for generating changelog ([ed6859b21d2cb2d](https://github.com/bsorrentino/java-async-generator/commit/ed6859b21d2cb2d886184f49b1fc1bd3a82a1760))
   




<!-- "name: v2.0.1" is a release tag -->

## [v2.0.1](https://github.com/bsorrentino/java-async-generator/releases/tag/v2.0.1) (2024-07-21)

### Features

 *  **AsyncGenerator**  add new Data.of() static method ([09de8d4ff324a8e](https://github.com/bsorrentino/java-async-generator/commit/09de8d4ff324a8eda81f712eb4cdbc11b009fc33))
     > make easier to create Data instance don&#x27;t providing CompletableFuture
   

### Bug Fixes

 -  deployment signing ([80ae69ced67514c](https://github.com/bsorrentino/java-async-generator/commit/80ae69ced67514c104742c4430067ed2939238fb))


### Documentation

 -  update javadoc ([dcbd81f1fffae8e](https://github.com/bsorrentino/java-async-generator/commit/dcbd81f1fffae8e27ee6185232511401755dea05))

 -  update changelog ([05cfc8f2b448cd9](https://github.com/bsorrentino/java-async-generator/commit/05cfc8f2b448cd9a01537e867b74e9657eb8af01))


### Refactor

 -  **AsyncGeneratorQuest**  deprecate method of( queue, executor, consumer) ([73d3ed4b65b79f6](https://github.com/bsorrentino/java-async-generator/commit/73d3ed4b65b79f668c239b31632537270843c86c))
    > now the standard is of( queue, consumer, executor )


### ALM 

 -  move to the next release ([61fc7accf77b410](https://github.com/bsorrentino/java-async-generator/commit/61fc7accf77b4102dabc13854a87f762760b2c70))
   






<!-- "name: v2.0.0" is a release tag -->

## [v2.0.0](https://github.com/bsorrentino/java-async-generator/releases/tag/v2.0.0) (2024-07-15)

### Features

 *  allow using executor on async implementation. by default ForkJoinPool.commonPool() ([ec470928c243ae9](https://github.com/bsorrentino/java-async-generator/commit/ec470928c243ae969a592689a17ddabeb0445e59))
     > - forEachAsync
     > - collectAsync
   


### Documentation

 -  update javadocs ([07798a4bd1d4692](https://github.com/bsorrentino/java-async-generator/commit/07798a4bd1d469278df70ce5ca38a71a14374685))

 -  update javadocs ([ec893b8bb4d702b](https://github.com/bsorrentino/java-async-generator/commit/ec893b8bb4d702b60cd5ff8700818719ffc50cc5))

 -  add javadoc ([ae7b39f259ce716](https://github.com/bsorrentino/java-async-generator/commit/ae7b39f259ce716967ab0f6b5d947f0893b6da95))

 -  add site support ([0801ee206b2031f](https://github.com/bsorrentino/java-async-generator/commit/0801ee206b2031f35e28fe75e3866010485038bb))

 -  update readme ([ae2972adf37f99a](https://github.com/bsorrentino/java-async-generator/commit/ae2972adf37f99a22f2f38b578419d05edb1fa55))

 -  add changelog ([683b8b74be21c86](https://github.com/bsorrentino/java-async-generator/commit/683b8b74be21c865483a408111a0b6969fe8bc7d))



### ALM 

 -  refine jdk8 support ([f8d6870c18ba8d2](https://github.com/bsorrentino/java-async-generator/commit/f8d6870c18ba8d205e58b30414d6286d40b21f51))
   
 -  add jdk8 classifier ([1a5e66caf4877c3](https://github.com/bsorrentino/java-async-generator/commit/1a5e66caf4877c347232b007e563d776b6625ad7))
   
 -  fix deployment process to maven central ([4ec64ec51d1d7f0](https://github.com/bsorrentino/java-async-generator/commit/4ec64ec51d1d7f02f1b638b5ca011d450bf533ce))
   
 -  add description in pom ([2d1c0af0dce602a](https://github.com/bsorrentino/java-async-generator/commit/2d1c0af0dce602a55dd638364867c2c647ca8838))
   


### Continuous Integration

 -  update deploy-github-pages.yml ([540441b71c9c8ce](https://github.com/bsorrentino/java-async-generator/commit/540441b71c9c8ce791412960f40bfb10aaf2fa53))
   




<!-- "name: v1.0.0-jdk11" is a release tag -->

## [v1.0.0-jdk11](https://github.com/bsorrentino/java-async-generator/releases/tag/v1.0.0-jdk11) (2024-04-24)

### Features

 *  add error propagation ([9468dd1a995d214](https://github.com/bsorrentino/java-async-generator/commit/9468dd1a995d2145eaccc92e12be90a08b131d70))
   


### Documentation

 -  add javadoc ([554c5f70ee13599](https://github.com/bsorrentino/java-async-generator/commit/554c5f70ee13599db4cdd3902df9a97fec85e115))

 -  update readme ([dfdbd70c3da9bc8](https://github.com/bsorrentino/java-async-generator/commit/dfdbd70c3da9bc84d9f861e3668bd85becf59c4e))

 -  update README.md ([8824be491f16def](https://github.com/bsorrentino/java-async-generator/commit/8824be491f16def904ace011eabcf29f0c29572f))


### Refactor

 -  clean code ([b95ffe647e23b1f](https://github.com/bsorrentino/java-async-generator/commit/b95ffe647e23b1f964d2a05ddc675a5c0d87744e))
   
 -  simplify BlockingQueue ([72aee2f5d9e806f](https://github.com/bsorrentino/java-async-generator/commit/72aee2f5d9e806ffcd3b212836ba51364317ec43))
   
 -  simplify interaction with collections ([bed45e744511e76](https://github.com/bsorrentino/java-async-generator/commit/bed45e744511e76660f1aba163c9b82f77ac9226))
    > add static methods empty, map, collect, toCompletableFuture

 -  consider completablefuture as argument ([d1310ceafddc653](https://github.com/bsorrentino/java-async-generator/commit/d1310ceafddc65393db037cb4afb63f03157a863))
   

### ALM 

 -  add build script using npm ([0e071c0423b2cc5](https://github.com/bsorrentino/java-async-generator/commit/0e071c0423b2cc50f507a581fce27e2b32209474))
   
 -  add changelog management ([6ee16713630faa1](https://github.com/bsorrentino/java-async-generator/commit/6ee16713630faa15c9c21b5205f201ab5edcc865))
   
 -  move to next version ([d3faef43359935c](https://github.com/bsorrentino/java-async-generator/commit/d3faef43359935c0e3b8d4acec9b48a135334f4e))
   
 -  add support for classifier ([44add67e66f96ec](https://github.com/bsorrentino/java-async-generator/commit/44add67e66f96ec636fa7ef4e18631ab1e8e952a))
   
 -  setup deploy asset to maven repo ([0a81096c4d43773](https://github.com/bsorrentino/java-async-generator/commit/0a81096c4d43773368f0305edc9fc8df12500c80))
   
 -  update pom information ([02e8557d7aa17d2](https://github.com/bsorrentino/java-async-generator/commit/02e8557d7aa17d287caf6d654e33d599e76dc560))
   




