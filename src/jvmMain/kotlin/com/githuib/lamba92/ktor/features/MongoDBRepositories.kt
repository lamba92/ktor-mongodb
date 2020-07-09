package com.githuib.lamba92.ktor.features

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.slf4j.Logger
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class MongoDBRepositories private constructor(val configuration: Configuration) {

    companion object Feature : ApplicationFeature<Application, Configuration, MongoDBRepositories> {

        internal const val collectionIdTag = "collectionIdTag"

        override val key = AttributeKey<MongoDBRepositories>("MongoDBRepositories")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): MongoDBRepositories {
            val conf = Configuration(pipeline.log).apply(configure)
            assert(conf.repositoryPath.withoutWhitespaces.isNotBlank()) { "Repository path cannot be blank or empty" }
            val feature = MongoDBRepositories(conf)

            pipeline.routing {
                conf.builtRoutes.forEach {
                    route(conf.repositoryPath, it)
                }
            }

            return feature
        }

    }

    @MongoDBRepositoriesDSL
    class Configuration(val logger: Logger) {

        @PublishedApi
        internal val entitiesConfigurationMap: MutableMap<Pair<String, HttpMethod>, Route.() -> Unit> = mutableMapOf()

        internal val builtRoutes
            get() = entitiesConfigurationMap.values.toList()

        var repositoryPath: String = "repositories"
            set(value) {
                assert(value.withoutWhitespaces.isNotBlank()) { "Repository path cannot be blank or empty" }
                field = value
            }

        inline fun <reified T : Any> collection(
            database: CoroutineDatabase,
            collectionName: String = T::class.simpleName!!.toLowerCase(),
            httpMethodsConf: CollectionSetup<T>.() -> Unit
        ) {

            runBlocking {
                if (collectionName !in database.listCollectionNames())
                    database.createCollection(collectionName)
            }

            CollectionSetup<T>(T::class.simpleName!!.toLowerCase())
                .apply(httpMethodsConf)
                .apply {
                    val logBuilder = StringBuilder()
                    assert(collectionPath.withoutWhitespaces.isNotBlank()) { "${T::class.simpleName} path cannot be blank or empty" }
                    logBuilder.appendln("Building methods for entity ${T::class.simpleName}:")
                    configuredMethods.forEach { (httpMethod, behaviour) ->
                        this@Configuration.entitiesConfigurationMap[collectionPath.withoutWhitespaces, httpMethod] =
                            getRouteActions(
                                httpMethod,
                                collectionName,
                                database,
                                behaviour.dataInterceptor
                            ).toRoute(
                                collectionPath.withoutWhitespaces,
                                httpMethod,
                                behaviour.isAuthenticated,
                                behaviour.authNames
                            )
                        logBuilder.appendln(
                            "     - ${httpMethod.value.padEnd(7)} | ${this@Configuration.repositoryPath.withoutWhitespaces}/${collectionPath.withoutWhitespaces} " +
                                    "| Authentication realm/s: ${behaviour.authNames.joinToString { it ?: "Default" }}"
                        )
                    }
                    this@Configuration.logger.info(logBuilder.toString())
                }
        }

        @MongoDBRepositoriesDSL
        class CollectionSetup<T : Any>(
            var collectionPath: String
        ) {

            @PublishedApi
            internal val configuredMethods = mutableMapOf<HttpMethod, Behaviour<T>>()

            @OptIn(InternalAPI::class)
            fun addEndpoint(httpMethod: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) {
                configuredMethods[httpMethod] = Behaviour<T>()
                    .apply(behaviourConfiguration)
            }

            fun addEndpoints(vararg httpMethods: HttpMethod, behaviourConfiguration: Behaviour<T>.() -> Unit = {}) =
                httpMethods.forEach { addEndpoint(it, behaviourConfiguration) }

            data class Behaviour<T : Any>(
                var isAuthenticated: Boolean = false,
                var authNames: List<String?> = mutableListOf(null),
                var dataInterceptor: DataInterceptor<T> = { it }
            )

        }
    }

}
