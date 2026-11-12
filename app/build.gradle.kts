plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val miuixVersion = "0.9.4"

// Google OAuth client id for the Player tab's "Sign in with Google" (public value, not a secret).
// Set GOOGLE_CLIENT_ID in gradle.properties or as an environment variable; see README.
val googleClientId: String = (providers.gradleProperty("GOOGLE_CLIENT_ID").orNull ?: System.getenv("GOOGLE_CLIENT_ID") ?: "").trim()
val googleRedirectScheme = "com.googleusercontent.apps." + googleClientId.removeSuffix(".apps.googleusercontent.com").ifEmpty { "unconfigured" }

@Suppress("UnstableApiUsage")
android {
    namespace = "com.repixiv.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    defaultConfig {
        applicationId = "com.repixiv.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        manifestPlaceholders["googleRedirectScheme"] = googleRedirectScheme
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // UI: Miuix (https://github.com/compose-miuix-ui/miuix), Apache-2.0
    implementation("top.yukonga.miuix.kmp:miuix-ui:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-preference:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-icons:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-blur:$miuixVersion")
    implementation("top.yukonga.miuix.kmp:miuix-squircle:$miuixVersion")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
