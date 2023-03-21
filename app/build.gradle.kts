plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm()
    js(IR) {
        browser {
            runTask {
                devServer = devServer?.copy(
                    port = 9000,
                    proxy = mutableMapOf(
                        "/api/todos" to "http://localhost:8080"
                    )
                )
            }
        }
    }.binaries.executable()

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.extra["serializationVersion"]}")
                implementation("dev.fritz2:core:${project.extra["fritz2Version"]}")
                // implementation("dev.fritz2:headless:$fritz2Version") // optional
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-junit"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "dev.fritz2:lenses-annotation-processor:${project.extra["fritz2Version"]}")
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().all {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}

tasks.named("jsSourcesJar") {
    dependsOn("jsBrowserWebpack")
}
