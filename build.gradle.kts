plugins {
    id("org.owasp.dependencycheck") version "12.2.2" apply false
    `java-library`
    `maven-publish`
    signing
}

allprojects {
    apply(plugin = "org.owasp.dependencycheck")
}

configure<org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension> {
    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL.toString()
}

group   = "io.github.roebi"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // OTel API exposed as api() so callers can use Span / Tracer without re-declaring
    api(libs.opentelemetry.api)

    // SDK and exporter are implementation details of the bootstrap classes
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.exporter.otlp)

    // Test dependencies
    testImplementation(libs.opentelemetry.sdk.testing)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title"   to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

// ---------------------------------------------------------------------------
// Publishing - Maven Central via Sonatype Central Portal
//
// Required secrets in GitHub Actions / local gradle.properties:
//   OSSRH_USERNAME        - Sonatype Central Portal username (token)
//   OSSRH_PASSWORD        - Sonatype Central Portal password (token)
//   SIGNING_KEY_ID        - last 8 chars of GPG key fingerprint
//   SIGNING_KEY           - ASCII-armored GPG private key (export with:
//                           gpg --armor --export-secret-keys <KEY_ID>)
//   SIGNING_PASSWORD      - GPG key passphrase
// ---------------------------------------------------------------------------

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId    = "io.github.roebi"
            artifactId = "qf-fx-gui-lib-telemetry"
            version    = project.version.toString()

            pom {
                name        = "qf-fx-gui-lib-telemetry"
                description = "OpenTelemetry instrumentation for QFTest JavaFX GUI Library performance measurement"
                url         = "https://github.com/roebi/qf-fx-gui-lib-telemetry"

                licenses {
                    license {
                        name = "MIT License"
                        url  = "https://opensource.org/licenses/MIT"
                    }
                }

                developers {
                    developer {
                        id    = "roebi"
                        name  = "roebi"
                        email = "roebi@users.noreply.github.com"
                        url   = "https://github.com/roebi"
                    }
                }

                scm {
                    connection          = "scm:git:git://github.com/roebi/qf-fx-gui-lib-telemetry.git"
                    developerConnection = "scm:git:ssh://github.com/roebi/qf-fx-gui-lib-telemetry.git"
                    url                 = "https://github.com/roebi/qf-fx-gui-lib-telemetry"
                }
            }
        }
    }

    // Repository upload is handled by nmcp (settings.gradle.kts)
}

signing {
    val signingKeyId    = providers.gradleProperty("SIGNING_KEY_ID")
        .orElse(providers.environmentVariable("SIGNING_KEY_ID")).orNull
    val signingKey      = providers.gradleProperty("SIGNING_KEY")
        .orElse(providers.environmentVariable("SIGNING_KEY")).orNull
    val signingPassword = providers.gradleProperty("SIGNING_PASSWORD")
        .orElse(providers.environmentVariable("SIGNING_PASSWORD")).orNull

    if (signingKey != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications["mavenJava"])
    }
}
