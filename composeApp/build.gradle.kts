import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koalaplot.core)
            implementation(libs.filekit.core)
            // kotlin-csv doesn't support wasmJs — CSV parsing handled in-house (Unit 3)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.work.runtime)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.web.worker.driver)
            implementation(libs.ktor.client.js)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.3.2"))
            implementation(npm("sql.js", "1.11.0"))
            implementation(npm("copy-webpack-plugin", "12.0.2"))
        }
    }
}

sqldelight {
    databases {
        create("PortfolioDatabase") {
            packageName.set("app.portfoliotracker.data.database")
            generateAsync.set(true)
        }
    }
}

android {
    namespace = "app.portfoliotracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.portfoliotracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
