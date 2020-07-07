import com.githuib.lamba92.ktor.features.MongoDBRepositories
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.Test
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Tests : AbstractTest() {

    override fun <R> withTestApp(tests: TestApplicationEngine.() -> R) = withTestApplication({

        install(ContentNegotiation) {
            json()
        }

        install(MongoDBRepositories) {

            repositoryPath = "data"

            collection<TestData>(db, "testdata") {
                collectionPath = "testdatalol"
                addEndpoints(Get, Post, Put, Delete)
            }
        }

        routing {
            trace {
                application.log.debug(it.buildText())
            }
        }

    }, tests)

    @BeforeAll
    fun clearDB(): Unit = runBlocking {
        db.dropCollection("testdata")
        return@runBlocking
    }

    @Test
    @Order(1)
    fun `test put`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol/single", testData.first().toString()) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(2)
    fun `test get`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol/${testData.first()._id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(response.content?.asTestData(), testData.first())
        }
    }

    @Test
    @Order(3)
    fun `test delete`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol/${testData.first()._id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

}

