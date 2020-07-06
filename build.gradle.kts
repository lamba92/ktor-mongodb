@file:Suppress("SuspiciousCollectionReassignment")

import com.github.lamba92.gradle.utils.TRAVIS_TAG
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
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
}

group = "com.github.lamba92"
version = TRAVIS_TAG ?: "0.0.1"

repositories {
    mavenLocal()
    jcenter()
    maven("https://kotlin.bintray.com/ktor")
}

kotlin.target.compilations.all {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
}

dependencies {

    val ktorVersion: String by project

    implementation(kotlin("stdlib-jdk8"))

    implementation(ktor("server-core", ktorVersion))
    implementation(ktor("auth", ktorVersion))
    implementation("org.litote.kmongo", "kmongo-coroutine", "4.0.2")

    testImplementation(ktor("server-tests", ktorVersion))
    testImplementation(ktor("serialization", ktorVersion))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", "5.5.2")

}
tasks {
    test {
        useJUnitPlatform()
    }
}

prepareForPublication()
