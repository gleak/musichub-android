import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

/**
 * BASE_URL resolution order (first match wins):
 *  1. `-PBASE_URL_DEBUG=...` / `-PBASE_URL_RELEASE=...` / `-PBASE_URL=...`
 *     on the Gradle command line.
 *  2. `base.url.debug` / `base.url.release` / `base.url` in
 *     `local.properties` (gitignored — safe place for per-machine
 *     overrides like a LAN IP or a private dev backend).
 *  3. Debug: falls back to `http://10.0.2.2:8080` (the emulator's
 *     loopback to the host). Release: defaults to the production IP.
 */
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// HTTPS via Caddy + Let's Encrypt fronting the backend. The cleartext
// :8090 mapping stays open on QNAP during cutover; release builds can
// override this in `local.properties` (`base.url.release=...`) if a
// fallback to the legacy `http://194.116.60.68:8090` is needed for
// debugging.
val RELEASE_URL_DEFAULT = "https://92b70eb3-9758-47a7-a830-744a9d61f809.duckdns.org"
val SENTINEL_URL = "https://mediaplayer.invalid"

fun resolveBaseUrl(variantKey: String, fallback: String): String {
    val cliSpecific = project.findProperty("BASE_URL_${variantKey.uppercase()}") as String?
    val cliGeneric = project.findProperty("BASE_URL") as String?
    val propSpecific = localProps.getProperty("base.url.${variantKey.lowercase()}")
    val propGeneric = localProps.getProperty("base.url")
    return cliSpecific ?: cliGeneric ?: propSpecific ?: propGeneric ?: fallback
}

/**
 * Signing config resolution. All four keys must be present in
 * `local.properties` (gitignored) for `myConfig` to register; otherwise
 * the signing block is skipped — debug builds fall back to Android's
 * auto-generated debug keystore, and release assembly fails loudly,
 * which is the desired posture (release must be signed by the real cert).
 *
 * Keys:
 *   keystore.file=<absolute path>
 *   keystore.password=<store password>
 *   keystore.alias=<key alias>
 *   keystore.key.password=<key password>
 */
val keystoreFile = localProps.getProperty("keystore.file")
val keystorePassword = localProps.getProperty("keystore.password")
val keystoreAlias = localProps.getProperty("keystore.alias")
val keystoreKeyPassword = localProps.getProperty("keystore.key.password")
val hasKeystore = keystoreFile != null &&
    keystorePassword != null &&
    keystoreAlias != null &&
    keystoreKeyPassword != null &&
    file(keystoreFile).exists()

android {
    namespace = "com.mediaplayer.android"
    compileSdk = 36


    signingConfigs {
        if (hasKeystore) {
            create("myConfig") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                keyAlias = keystoreAlias
                keyPassword = keystoreKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.mediaplayer.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 112
        versionName = "0.20.11"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        val GOOGLE_WEB_CLIENT_ID = localProps.getProperty(
            "google.web.client.id",
            "447608520923-gcuoeisusmvvjgh0g0pe2jc8ickctmcc.apps.googleusercontent.com"
        )
        debug {
            if (hasKeystore) signingConfig = signingConfigs.getByName("myConfig")
            val baseUrl = resolveBaseUrl("debug", fallback = "http://10.0.2.2:8080")
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$GOOGLE_WEB_CLIENT_ID\"")
            isMinifyEnabled = false
        }
        release {
            if (hasKeystore) signingConfig = signingConfigs.getByName("myConfig")
            val baseUrl = resolveBaseUrl("release", fallback = RELEASE_URL_DEFAULT)
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$GOOGLE_WEB_CLIENT_ID\"")
            isMinifyEnabled = true
            // R8 also strips unreferenced resources (drawables, strings, layouts)
            // from the packaged APK once minification has identified what code
            // survives. Pairs with isMinifyEnabled — alone it is a no-op.
            isShrinkResources = true
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

    // Build outputs name themselves `mediaplayer-<versionCode>-<versionName>.apk`
    // so the backend's update scanner can parse + sort filenames directly.
    // Drop the produced file into the server's app-update directory and
    // it surfaces as the latest manifest automatically.
    applicationVariants.all {
        outputs.all {
            val out = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            out.outputFileName = "mediaplayer-${defaultConfig.versionCode}-${defaultConfig.versionName}.apk"
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
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines. `-guava` bridges Media3's ListenableFuture-based
    // MediaLibrarySession.Callback to `suspend fun` via `future { ... }`.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

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
    // M10: disk cache for the last N tracks + background prefetch of
    // queue neighbours. Needs StandaloneDatabaseProvider for SimpleCache's
    // on-disk index.
    implementation(libs.media3.database)
    implementation(libs.media3.ui)

    // Navigation — top-level NavHost for Search / Playlists tabs (M6).
    implementation(libs.androidx.navigation.compose)

    // Drag-to-reorder for playlist songs LazyColumn.
    implementation(libs.reorderable)

    // Google Sign-In via Credential Manager (no Firebase / google-services.json needed).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // DataStore — persists "has signed in" flag across launches.
    implementation(libs.androidx.datastore.preferences)

    // Palette — extracts dominant color from cover art for gradient backdrops.
    implementation(libs.androidx.palette)

    // Glance — Jetpack Compose-style API for AppWidgets. Backs the
    // Now Playing + Quick Launch home-screen widgets.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}

/**
 * Guard: block any release-variant assembly when the configured URL is
 * explicitly set to the sentinel invalid value.
 */
val releaseUrlCheck = tasks.register("releaseUrlCheck") {
    group = "verification"
    description = "Fails if the release BASE_URL is explicitly invalid."

    val resolved = resolveBaseUrl("release", fallback = RELEASE_URL_DEFAULT)
    val sentinel = SENTINEL_URL

    doLast {
        if (resolved == sentinel) {
            throw GradleException(
                "Release build has an invalid BASE_URL ($sentinel). " +
                "Please configure a valid URL in local.properties or via command line."
            )
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        dependsOn(releaseUrlCheck)
    }
