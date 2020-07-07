package com.githuib.lamba92.ktor.features

import com.githuib.lamba92.ktor.features.EndpointMultiplicity.MULTIPLE
import com.githuib.lamba92.ktor.features.EndpointMultiplicity.SINGLE
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.coroutine.CoroutineDatabase

@DslMarker
annotation class MongoDBRepositoriesDSL

val String.withoutWhitespaces
    get() = filter { !it.isWhitespace() }

enum class EndpointMultiplicity {
    SINGLE, MULTIPLE
}

val ApplicationCall.documentId
    get() = parameters[MongoDBRepositories.collectionIdTag]
        ?: error("Missing ${MongoDBRepositories.collectionIdTag} from url")

suspend fun ApplicationCall.receiveEntityIds() =
    receive<List<String>>()


@InternalAPI
inline fun <reified T : Any> getRouteActions(
    httpMethod: HttpMethod,
    collectionName: String,
    database: CoroutineDatabase,
    crossinline customAction: RestRepositoryInterceptor<T>
): InterceptorsContainer {
    val single: suspend (PipelineContext<Unit, ApplicationCall>, Unit) -> Unit = {
        getRouteActions(
            SINGLE,
            httpMethod,
            database,
            collectionName,
            customAction
        )
    }()
    val multiple: suspend (PipelineContext<Unit, ApplicationCall>, Unit) -> Unit = {
        getRouteActions(
            MULTIPLE,
            httpMethod,
            database,
            collectionName,
            customAction
        )
    }()
    return InterceptorsContainer(
        single,
        multiple
    )
}

@InternalAPI
inline fun <reified T : Any> getRouteActions(
    endpointMultiplicity: EndpointMultiplicity,
    httpMethod: HttpMethod,
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: RestRepositoryInterceptor<T>
): PipelineInterceptor<Unit, ApplicationCall> = when (endpointMultiplicity) {
    SINGLE -> when (httpMethod) {
        Get -> httpGetDefaultSingleItemBehaviour<T>(database, collectionName, customAction)
        Post -> httpPostDefaultSingleItemBehaviour<T>(database, collectionName, customAction)
        Put -> httpPutDefaultSingleItemBehaviour<T>(database, collectionName, customAction)
        Delete -> httpDeleteDefaultSingleItemBehaviour<T>(database, collectionName, customAction)
        else -> error("Defaults handle only GET, POST, PUT and DELETE")
    }
    MULTIPLE -> when (httpMethod) {
        Get -> httpGetDefaultMultipleItemBehaviour<T>(database, collectionName, customAction)
        Post -> httpPostDefaultMultipleItemBehaviour<T>(database, collectionName, customAction)
        Put -> httpPutDefaultMultipleItemBehaviour<T>(database, collectionName, customAction)
        Delete -> httpDeleteDefaultMultipleItemBehaviour<T>(database, collectionName, customAction)
        else -> error("Defaults handle only GET, POST, PUT and DELETE")
    }
}

inline fun <reified T : Any> httpDeleteDefaultMultipleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    database.getCollection<T>(collectionName).run {
        call.receiveEntityIds().asFlow()
            .map { findOneById(it) }
            .filterNotNull()
            .map { customAction(it) }
            .filterNotNull()
            .map { deleteOneById(it) }
    }
    call.respond(HttpStatusCode.OK)
}

inline fun <reified T : Any> httpPutDefaultMultipleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    val docs = database.getCollection<T>(collectionName).let { collection ->
        call.receive<List<T>>()
            .mapNotNull { customAction(it) }
            .also { collection.insertMany(it) }
    }
    call.respond(docs)
}

inline fun <reified T : Any> httpPostDefaultMultipleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
) = httpPutDefaultMultipleItemBehaviour(database, collectionName, customAction)

inline fun <reified T : Any> httpGetDefaultMultipleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    val docs: List<T> = database.getCollection<T>(collectionName).run {
        call.receiveEntityIds().asFlow()
            .map { findOneById(it) }
            .filterNotNull()
            .map { customAction(it) }
            .filterNotNull()
            .toList()
    }
    call.respond(docs)
}

inline fun <reified T : Any> httpDeleteDefaultSingleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    database.getCollection<T>(collectionName).let { collection ->
        collection.findOneById(call.documentId)
            ?.let { customAction(it) }
            ?.let { collection.deleteOneById(call.documentId) }
            ?: call.respond(HttpStatusCode.Forbidden)
        call.respond(HttpStatusCode.OK)
    }
}

inline fun <reified T : Any> httpPutDefaultSingleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    customAction(call.receive())?.let {
        val c = database.getCollection<T>(collectionName)
        if (c.findOneById(call.documentId) == null) {
            database.getCollection<T>(collectionName)
                .insertOne(it)
            call.respond(HttpStatusCode.OK)
        } else
            null
    } ?: call.respond(HttpStatusCode.Forbidden)

}

inline fun <reified T : Any> httpPostDefaultSingleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    customAction(call.receive())?.also {
        val c = database.getCollection<T>(collectionName)
        if (c.findOneById(call.documentId) == null)
            c.insertOne(it)
        else
            c.updateOneById(call.documentId, it)
        call.respond(it)
    } ?: call.respond(HttpStatusCode.Forbidden)
}


inline fun <reified T : Any> httpGetDefaultSingleItemBehaviour(
    database: CoroutineDatabase,
    collectionName: String,
    crossinline customAction: PipelineContext<Unit, ApplicationCall>.(T) -> T?
): suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
    database.getCollection<T>(collectionName)
        .findOneById(call.documentId)
        ?.let { customAction(it) }
        ?.let { call.respond(it) }
        ?: call.respond(HttpStatusCode.NotFound)
}


operator fun <K1, K2, V> Map<Pair<K1, K2>, V>.get(key1: K1, key2: K2) =
    get(key1 to key2)

operator fun <K1, K2, V> MutableMap<Pair<K1, K2>, V>.set(key1: K1, key2: K2, value: V) {
    this[key1 to key2] = value
}

typealias RestRepositoryInterceptor<K> = PipelineContext<Unit, ApplicationCall>.(K) -> K?
