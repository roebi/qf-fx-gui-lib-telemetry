// nmcp (New Maven Central Publishing) - uses the Central Portal bundle upload API.
// See: https://gradleup.github.io/nmcp/
plugins {
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

rootProject.name = "qf-fx-gui-lib-telemetry"

nmcpSettings {
    centralPortal {
        // Credentials: generate a token at https://central.sonatype.com/
        // Set as environment variables OSSRH_USERNAME / OSSRH_PASSWORD
        // or in ~/.gradle/gradle.properties (never commit credentials to git)
        username = System.getenv("OSSRH_USERNAME") ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: ""

        // AUTOMATIC: nmcp waits for validation and releases automatically.
        // Switch to "USER_MANAGED" to review in the portal UI before releasing.
        publishingType = "AUTOMATIC"
    }
}

