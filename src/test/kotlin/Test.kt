import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.test.Test
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Tests {

    val testData = TestData("2oeufgnow3erugn", "wsjkrgbk")

    val db = KMongo.createClient("mongodb://192.168.1.158:27017")
        .getDatabase("test")
        .coroutine

    @BeforeAll
    fun clearDB(): Unit = runBlocking {
        db.dropCollection("testdata")
        return@runBlocking
    }

    private fun withTestApp(tests: TestApplicationEngine.() -> Unit) =
        testApp(db, tests)

    @Test
    @Order(1)
    fun `test put`(): Unit = withTestApp {
        handleRequest(Put, "data/testdatalol/single", testData.toString()) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    @Order(2)
    fun `test get`(): Unit = withTestApp {
        handleRequest(Get, "data/testdatalol/${testData._id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals(response.content?.asTestData(), testData)
        }
    }

    @Test
    @Order(3)
    fun `test delete`(): Unit = withTestApp {
        handleRequest(Delete, "data/testdatalol/${testData._id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

}

private fun String.asTestData() =
    Json(JsonConfiguration.Stable).parse(TestData.serializer(), this)
