import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "dev.brua"
// The release workflow passes the tag as -PreleaseVersion; 0.1.0 is the development placeholder.
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0")

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    useJUnit()
}

// Maven Central, from the public repository's release workflow: the version is the release tag, and signing
// happens where a key is configured (PUBLISHING.md); a local publishToMavenLocal needs none.
mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    coordinates("dev.brua", project.name, version.toString())
    pom {
        name.set("Brua for Kotlin: ${project.name}")
        description.set("The AssetResponse models, the variant selection rule and the request hints, for the JVM.")
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
