import com.githuib.lamba92.ktor.features.MongoDBRepositories
import io.ktor.application.*
import io.ktor.auth.*
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
class AuthenticatedTests : AbstractTest() {

    data class TestPrincipal(val username: String) : Principal

    override fun <R> withTestApp(tests: TestApplicationEngine.() -> R) = withTestApplication({

        install(ContentNegotiation) {
            json()
        }

        install(Authentication) {
            basic {
                validate {
                    if (it.name == "mario" && it.password == "super")
                        TestPrincipal(it.name)
                    else
                        null
                }
            }
        }

        install(MongoDBRepositories) {

            repositoryPath = "data"

            collection<TestData>(db, "testdata") {
                collectionPath = "testdatalol"
                addEndpoints(Get, Post, Put, Delete) {
                    isAuthenticated = true
                }
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
    fun `test put single`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol/single", testData.first().toString(), { addBasicAuth() }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(2)
    fun `test get single`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol/${testData.first()._id}") { addBasicAuth() }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(response.content?.asTestData(), testData.first())
        }
    }

    @Test
    @Order(3)
    fun `test delete single`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol/${testData.first()._id}") { addBasicAuth() }.apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(4)
    fun `test put multiple`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol", testData.toJson(), { addBasicAuth() }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(5)
    fun `test get multiple`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol", testData.asIdsJson(), { addBasicAuth() }) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(response.content?.asTestDataList()?.sortedBy { it._id }, testData.sortedBy { it._id })
        }
    }

    @Test
    @Order(6)
    fun `test delete multiple`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol", testData.asIdsJson(), { addBasicAuth() }) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(7)
    fun `test fail put single`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol/single", testData.first().toString()) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    @Order(8)
    fun `test fail get single`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol/${testData.first()._id}").apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    @Order(9)
    fun `test fail delete single`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol/${testData.first()._id}").apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    @Order(10)
    fun `test fail put multiple`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol", testData.toJson()) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    @Order(11)
    fun `test fail get multiple`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol", testData.asIdsJson()) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Test
    @Order(12)
    fun `test fail delete multiple`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol", testData.asIdsJson()) {
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

}

