# Changelog



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
   




