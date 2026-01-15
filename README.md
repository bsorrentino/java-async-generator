<a href="https://central.sonatype.com/artifact/org.bsc.async/async-generator"><img src="https://img.shields.io/maven-central/v/org.bsc.async/async-generator.svg">
</a>

# java-async-generator

A Java version of Javascript async generator. 
Idea is to create an iterator-like interface that emit elements as [CompletableFuture] ( the Java counterpart of Javascript Promise ) enabling asynchronous iteration over data 


## Releases 

**Note: ‼️**
> From release 3.0.0 the miminum supported Java version is the `Java 17` and
> will not be longer available the artifact `async-generator-jdk8`


## Installation

**Maven**
```xml
<dependency>
    <groupId>org.bsc.async</groupId>
    <artifactId>async-generator</artifactId>
    <version>4.0.0</version> <!-- Or the current snapshot version -->
</dependency>
```

## Samples

### Create an Async Generator to make multiple API calls
 
```java

AsyncGenerator<Response> makeMultipleApiCalls(List<RequestData> requestsData) {
    return AsyncGenerator.map(requestsData, requestData -> {

                CompletableFuture<Response> res = asyncApiCall( requestData );

                return res;
    });
    
}

List<RequestData> resquestsData = .... 

// can iterate using lambda function (Consumer)
makeMultipleApiCalls( resquestsData )
        .forEachAsync( response -> logger.info( "Api response: " + response ) )
        .join();

// can iterate using classic for( : )
AsyncGenerator<Response> generator = makeMultipleApiCalls( resquestsData );

for( Response response : generator ) {
    logger.info( "Api response: " + response )
}        

```
## Cancellation

Take a look the new [Cancellation](CANCELLATION.md) feature

## Articles:

* [How to stream data over HTTP using Java Servlet and Fetch API](https://bsorrentino.github.io/bsorrentino/web/2024/07/21/how-to-stream-data-in-java.html)

[CompletableFuture]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html
