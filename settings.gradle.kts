pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    resolutionStrategy {
        eachPlugin {
            val kotlinVersion: String by settings
            val bintrayVersion: String by settings
            when (requested.id.id) {
                "org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.jvm" ->
                    useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                "org.jetbrains.kotlin.plugin.serialization" ->
                    useModule("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
                "com.jfrog.bintray" -> useModule("com.jfrog.bintray.gradle:gradle-bintray-plugin:$bintrayVersion")
            }
        }
    }
}

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(System.getenv("CI")?.toBoolean() == true)
    }
}

rootProject.name = "ktor-mongodb"
