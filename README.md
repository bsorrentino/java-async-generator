<a href="https://central.sonatype.com/artifact/org.bsc.async/async-generator"><img src="https://img.shields.io/maven-central/v/org.bsc.async/async-generator.svg">
</a>
# java-async-generator

A Java version of Javascript async generator. 
Idea is to create an iterator-like interface that emit elements as [CompletableFuture] ( the Java counterpart of Javascript Promise ) enabling asynchronous iteration over data 

# Samples

## Create an Async Generator to make multiple API calls
 
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


[CompletableFuture]: https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/CompletableFuture.html
