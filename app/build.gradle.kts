plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "neumann.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dividetask.neumann"
        minSdk = 26
        targetSdk = 34
        // versionCode: timestamp-based, always increasing across branches.
        // versionName: predicted APK filename (vMAJOR.MINOR.apk = newest committed
        // APK minor + 1), a per-commit letter (a, b, c… for each commit pushed
        // before a new APK lands; resets to 'a' once a new APK is built), and the
        // 4-char hash of the commit Gradle built from. The first two are set by
        // hand; the hash is added at build time because a commit can't contain
        // its own hash.
        versionCode = (System.currentTimeMillis() / 60000).toInt()
        val versionBase = "v0.005 c"
        val commitHash = try {
            val process = Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=4", "HEAD"))
            process.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
        } catch (_: Exception) { "unknown" }
        versionName = "$versionBase ($commitHash)"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // The game data in data/ ships as the app's assets.
    sourceSets["main"].assets.srcDirs("../data")
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
