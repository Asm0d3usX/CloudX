import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3") // Jangan diganti ke versi terbaru, karena ada masalah dengan versi terbaru
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(configuration: CloudstreamExtension.() -> Unit) = extensions.getByName<CloudstreamExtension>("cloudstream").configuration()

fun Project.android(configuration: BaseExtension.() -> Unit) = extensions.getByName<BaseExtension>("android").configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(System.getenv("GITHUB_REPOSITORY") ?: "https://github.com/Asm0d3usX/CloudX")
        authors = listOf("Asm0d3usX")
    }

    android {
        namespace = "com.asm0d3usx"

        defaultConfig {
            minSdk = 26
            compileSdkVersion(35)
            targetSdk = 35

        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }


        tasks.withType<KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
                freeCompilerArgs.addAll(
                    "-Xno-call-assertions",
                    "-Xno-param-assertions",
                    "-Xno-receiver-assertions"
                )
                allWarningsAsErrors.set(false)
            }
        }
    }

    dependencies {
        val cloudstream by configurations
        val implementation by configurations
        
        // Cloudstream dependencies
        cloudstream("com.lagradost:cloudstream3:pre-release")

        // Other dependencies
        implementation(kotlin("stdlib")) // Untuk Kotlin Standard Library
        implementation("com.github.Blatzar:NiceHttp:0.4.13") // Untuk HTTP requests
        implementation("org.jsoup:jsoup:1.19.1") // Untuk parsing HTML
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0") // JANGAN DIGANTI ke versi terbaru, karena ada masalah dengan versi terbaru
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0") // Untuk serialisasi/deserialisasi JSON
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1") // Untuk coroutines di Android
        implementation("org.mozilla:rhino:1.8.0") // Untuk JavaScript engine
        implementation("me.xdrop:fuzzywuzzy:1.4.0") // Untuk fuzzy matching
        implementation("com.google.code.gson:gson:2.11.0") // Untuk serialisasi/deserialisasi JSON
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0") // Untuk serialisasi/deserialisasi JSON
        implementation("app.cash.quickjs:quickjs-android:0.9.2") // Untuk JavaScript engine
        implementation("com.squareup.okhttp3:okhttp:4.12.0") // Untuk HTTP requests
        implementation("androidx.core:core-ktx:1.16.0") // Untuk Log dan utilitas Android
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1") // Untuk coroutines

    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
