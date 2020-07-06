import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class TestData(val _id: String, val s: String) {
    override fun toString() =
        Json(JsonConfiguration.Stable).stringify(serializer(), this)
}
