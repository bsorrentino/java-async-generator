# java-async-generator
a Java version of Javascript async generator

# Samples

## Create an Async Generator to make multiple API calls
 
```java

AsyncGenerator<Response> makeMultipleApiCalls(List<RequestData> resquestData) {
    return AsyncGenerator.map(requestData, requestData -> {

                CompletableFuture<Response> response = asyncApiCall( requestData );

                return response;
    });
    
}


List<RequestData> resquestData = .... 

makeMultipleApiCalls( requestData )
        .forEachAsync( response -> logger.info( "Api response: " + response ) )
        .join();
        



```
