// Plugins go on one shared classpath so the Kotlin plugin can see the Android
// plugin. The Android plugin is only added when an Android SDK is present
// (see settings.gradle.kts); the agent's environment has no SDK and cannot
// reach Google's Maven repository.
buildscript {
    val sdkFound = rootProject.file("local.properties").exists() ||
        System.getenv("ANDROID_HOME") != null ||
        System.getenv("ANDROID_SDK_ROOT") != null
    repositories {
        if (sdkFound) google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.20")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.0.20")
        if (sdkFound) classpath("com.android.tools.build:gradle:8.5.2")
    }
}
