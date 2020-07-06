import com.githuib.lamba92.ktor.features.MongoDBRepositories
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.testing.*
import org.litote.kmongo.coroutine.CoroutineDatabase

fun <R> testApp(db: CoroutineDatabase, tests: TestApplicationEngine.() -> R) = withTestApplication({

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

    routing {
        trace {
            application.log.debug(it.buildText())
        }
    }

}, tests)

fun TestApplicationRequest.setContentType(contentType: ContentType) =
    addHeader("Content-Type", contentType.toString())

fun TestApplicationEngine.handleRequest(
    method: HttpMethod,
    uri: String,
    body: String,
    action: TestApplicationCall.() -> Unit
) = handleRequest(method, uri) {
    setContentType(ContentType.Application.Json)
    setBody(body)
}.apply(action)
