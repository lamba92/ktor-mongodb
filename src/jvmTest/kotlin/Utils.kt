import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun TestApplicationRequest.setContentType(contentType: ContentType) =
    addHeader(HttpHeaders.ContentType, contentType.toString())

@OptIn(InternalAPI::class)
fun TestApplicationRequest.addBasicAuth(user: String = "mario", pass: String = "super") =
    addHeader(HttpHeaders.Authorization, "Basic " + "$user:$pass".encodeBase64())

val serializer by lazy { Json(JsonConfiguration.Stable) }

fun TestApplicationEngine.handleRequest(
    method: HttpMethod,
    uri: String,
    body: String,
    conf: TestApplicationRequest.() -> Unit = {},
    action: TestApplicationCall.() -> Unit
) = handleRequest(method, uri) {
    setContentType(ContentType.Application.Json)
    setBody(body)
    conf()
}.apply(action)

fun String.asTestData() =
    serializer.parse(TestData.serializer(), this)

fun String.asTestDataList() =
    serializer.parse(ListSerializer(TestData.serializer()), this)

fun List<TestData>.toJson() =
    serializer.stringify(ListSerializer(TestData.serializer()), this)

fun List<TestData>.asIdsJson() =
    map { it._id }.let { serializer.stringify(ListSerializer(String.serializer()), it) }
