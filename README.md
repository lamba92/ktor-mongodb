# MongoDB repositories for Ktor

This project allows to easily build routes for Ktor to expose particular collections from one or many MongoDBs.

# Usage 

```kotlin
// use whatever serialization method you want but use one!
install(ContentNegotiation) {
    json()
}

install(MongoDBRepositories) {

    repositoryPath = "data"

    collection<TestData>(db) {
        collectionPath = "testdatalol"
        addEndpoints(HttpMethod.Get, HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete) {
            isAuthenticated = false
        }
    }
}
```

Here gets exposed endpoints:
 - `GET`: `/data/testdatalol/{_id}` the response is a single JSON.
 - `DELETE`: `/data/testdatalol/{_id}` no body, just OK response.
 - `POST`, `PUT`: `/data/testdatalol/single` where the body is a single JSON.
 
 - `GET`: `/data/testdatalol` where the request body should be a list of `_id`s and the response is a list of JSONs.
 - `DELETE`: `/data/testdatalol` where the request body should be a list of `_id`s and the response is empty with OK http status.
 
