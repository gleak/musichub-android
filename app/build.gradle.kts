import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * BASE_URL resolution order (first match wins):
 *  1. `-PBASE_URL_DEBUG=...` / `-PBASE_URL_RELEASE=...` / `-PBASE_URL=...`
 *     on the Gradle command line.
 *  2. `base.url.debug` / `base.url.release` / `base.url` in
 *     `local.properties` (gitignored — safe place for per-machine
 *     overrides like a LAN IP or a private dev backend).
 *  3. Debug: falls back to `http://10.0.2.2:8080` (the emulator's
 *     loopback to the host). Release: no implicit fallback — the
 *     sentinel `https://mediaplayer.invalid` is baked in and the
 *     `releaseUrlCheck` task below fails `assembleRelease` if nothing
 *     has overridden it.
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val RELEASE_URL_PLACEHOLDER = "https://mediaplayer.invalid"

fun resolveBaseUrl(variantKey: String, fallback: String): String {
    val cliSpecific = project.findProperty("BASE_URL_${variantKey.uppercase()}") as String?
    val cliGeneric = project.findProperty("BASE_URL") as String?
    val propSpecific = localProps.getProperty("base.url.${variantKey.lowercase()}")
    val propGeneric = localProps.getProperty("base.url")
    return cliSpecific ?: cliGeneric ?: propSpecific ?: propGeneric ?: fallback
}

android {
    namespace = "com.mediaplayer.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediaplayer.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val baseUrl = resolveBaseUrl("debug", fallback = "http://10.0.2.2:8080")
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            isMinifyEnabled = false
        }
        release {
            // Configure always succeeds — the guard runs when
            // `assembleRelease` is actually requested (see below).
            val baseUrl = resolveBaseUrl("release", fallback = RELEASE_URL_PLACEHOLDER)
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
    // Core + lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking + JSON
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Playback — ExoPlayer via a bound MediaSessionService so audio keeps
    // playing in the background with a media notification + lock-screen
    // controls. OkHttp DataSource lets us reuse Network.okHttp.
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.okhttp)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    // Navigation — top-level NavHost for Search / Playlists tabs (M6).
    implementation(libs.androidx.navigation.compose)
}

/**
 * Guard: block any release-variant assembly when the configured URL is
 * still the placeholder. Hooks into every `assembleRelease`-ish task via
 * `afterEvaluate` so it runs before Android's APK packaging — catches
 * CI mis-configurations before they ship.
 */
val releaseUrlCheck = tasks.register("releaseUrlCheck") {
    group = "verification"
    description = "Fails if the release BASE_URL is still the placeholder."
    doLast {
        val resolved = resolveBaseUrl("release", fallback = RELEASE_URL_PLACEHOLDER)
        if (resolved == RELEASE_URL_PLACEHOLDER) {
            throw GradleException(
                "Release build has no BASE_URL. Set `base.url.release=https://your.host` " +
                    "in local.properties, or pass `-PBASE_URL_RELEASE=…` on the command " +
                    "line. Debug builds are unaffected."
            )
        }
    }
}

afterEvaluate {
    tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
        .configureEach { dependsOn(releaseUrlCheck) }
}

