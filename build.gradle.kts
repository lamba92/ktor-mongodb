@file:Suppress("SuspiciousCollectionReassignment", "COMPATIBILITY_WARNING", "UNUSED_VARIABLE")

import com.github.lamba92.gradle.utils.TRAVIS_TAG
import com.github.lamba92.gradle.utils.implementation
import com.github.lamba92.gradle.utils.ktor
import com.github.lamba92.gradle.utils.prepareForPublication

buildscript {
    repositories {
        maven("https://dl.bintray.com/lamba92/com.github.lamba92")
        jcenter()
        google()
    }
    dependencies {
        classpath("com.github.lamba92", "lamba-gradle-utils", "1.0.6")
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
    id("com.jfrog.bintray")
}

group = "com.github.lamba92"
version = TRAVIS_TAG ?: "0.0.1"

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
            }
        }
    }

    sourceSets {

        val ktorVersion: String by project
        val kmongoVersion: String by project
        val jupyterVersion: String by project

        val jvmMain by getting {
            dependencies {

                implementation(kotlin("stdlib-jdk8"))

                implementation(ktor("server-core", ktorVersion))
                implementation(ktor("auth", ktorVersion))
                implementation("org.litote.kmongo", "kmongo-coroutine-serialization", kmongoVersion)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(ktor("server-tests", ktorVersion))
                implementation(ktor("serialization", ktorVersion))
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter", "junit-jupiter-engine", jupyterVersion)
            }
        }
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}

prepareForPublication()
