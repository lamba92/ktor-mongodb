import io.ktor.server.testing.*
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.random.Random

abstract class AbstractTest {

    private val dbUrl: String by System.getenv().withDefault { "mongodb://192.168.1.159:27017" }

    protected val db = KMongo.createClient(dbUrl)
        .getDatabase("test")
        .coroutine

    protected val testData = (0..10).map {
        TestData(Random.nextInt().toString(), Random.nextInt().toString(), TestEnum.values().random())
    }

    protected abstract fun <R> withTestApp(tests: TestApplicationEngine.() -> R): R


}
