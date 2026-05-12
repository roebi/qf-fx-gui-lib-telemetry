plugins {
    `java-library`
    `maven-publish`
    signing
    alias(libs.plugins.owasp.dependencycheck)
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
// Supply-chain audit - OWASP Dependency Check
//
// Required secret: NVD_API_KEY  (generate at https://nvd.nist.gov/developers/request-an-api-key)
// Without the key the NVD data download is heavily rate-limited and VERY slow.
//
// Run locally:
//   NVD_API_KEY=<your-key> ./gradlew dependencyCheckAnalyze
// ---------------------------------------------------------------------------

dependencyCheck {
    // Read the key from the environment; empty string = no key (slow but not broken)
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""

    // Fail the build on CVSS score >= 7 (HIGH or CRITICAL)
    failBuildOnCVSS = 7.0f

    // Keep the NVD data cache between runs (CI: cache the suppressions dir)
    autoUpdate = true

    // HTML + JSON reports under build/reports/dependency-check/
    formats = listOf("HTML", "JSON")

    // Suppress false positives by adding entries to this file as needed
    suppressionFile = "dependency-check-suppressions.xml"
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
