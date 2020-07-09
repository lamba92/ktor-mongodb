package com.githuib.lamba92.ktor.features

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.HttpMethod.Companion.Delete
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpMethod.Companion.Post
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.routing.*
import io.ktor.util.pipeline.*

data class InterceptorsContainer(
    val single: PipelineInterceptor<Unit, ApplicationCall>,
    val multiple: PipelineInterceptor<Unit, ApplicationCall>
) {
    fun toRoute(
        entityPath: String,
        httpMethod: HttpMethod,
        isAuthenticated: Boolean,
        authNames: List<String?>
    ): Route.() -> Unit = if (isAuthenticated) {
        { authenticate(*authNames.toTypedArray(), build = buildRoutes(entityPath, httpMethod)) }
    } else {
        { buildRoutes(entityPath, httpMethod)() }
    }

    private fun buildRoutes(collectionPath: String, httpMethod: HttpMethod): Route.() -> Unit = {
        route(collectionPath) {
            route("{${MongoDBRepositories.collectionIdTag}}") {
                when (httpMethod) {
                    Get, Delete -> method(httpMethod) { handle(single) }
                    Post, Put -> method(httpMethod) {
                        contentType(ContentType.Application.Json) {
                            handle(single)
                        }
                    }
                }
            }
            method(httpMethod) {
                contentType(ContentType.Application.Json) {
                    handle(multiple)
                }
            }
        }
    }

}
