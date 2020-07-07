import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun TestApplicationRequest.setContentType(contentType: ContentType) =
    addHeader("Content-Type", contentType.toString())

val serializer by lazy { Json(JsonConfiguration.Stable) }

fun TestApplicationEngine.handleRequest(
    method: HttpMethod,
    uri: String,
    body: String,
    action: TestApplicationCall.() -> Unit
) = handleRequest(method, uri) {
    setContentType(ContentType.Application.Json)
    setBody(body)
}.apply(action)

fun String.asTestData() =
    serializer.parse(TestData.serializer(), this)

fun String.asTestDataList() =
    serializer.parse(ListSerializer(TestData.serializer()), this)
