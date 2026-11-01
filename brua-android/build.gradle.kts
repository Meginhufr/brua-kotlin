plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

group = "dev.brua"
// The release workflow passes the tag as -PreleaseVersion; 0.1.0 is the development placeholder.
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0")

android {
    namespace = "dev.brua.android"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Robolectric reaches into the JDK's file descriptor internals, closed since Java 17.
            all { test -> test.jvmArgs("--add-opens", "java.base/jdk.internal.access=ALL-UNNAMED") }
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":brua"))
    api(libs.coil.compose)
    api(libs.okhttp)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlin.test.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Maven Central, from the public repository's release workflow: the version is the release tag, and signing
// happens where a key is configured (PUBLISHING.md); a local publishToMavenLocal needs none.
mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("dev.brua", project.name, version.toString())
    pom {
        name.set("Brua for Kotlin: ${project.name}")
        description.set("A BruaImage composable for Jetpack Compose, the device state and an OkHttp interceptor.")
        url.set("https://github.com/Meginhufr/brua-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }
        developers {
            developer {
                id.set("meginhufr")
                name.set("Meginhufr AS")
                url.set("https://brua.dev")
            }
        }
        scm {
            url.set("https://github.com/Meginhufr/brua-kotlin")
            connection.set("scm:git:git://github.com/Meginhufr/brua-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/Meginhufr/brua-kotlin.git")
        }
    }
}
