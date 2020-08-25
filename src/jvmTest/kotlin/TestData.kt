import kotlinx.serialization.Serializable

enum class TestEnum {
    A, B, C
}

@Serializable
data class TestData(val _id: String, val s: String, val e: TestEnum) {
    override fun toString() =
        serializer.encodeToString(serializer(), this)
}
