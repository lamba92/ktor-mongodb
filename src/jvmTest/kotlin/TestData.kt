import kotlinx.serialization.Serializable

@Serializable
data class TestData(val _id: String, val s: String) {
    override fun toString() =
        serializer.stringify(serializer(), this)
}
