// The app module needs the Android SDK. Without one (the agent's
// environment), only :core is built so its unit tests can still run.
val sdkFound = file("local.properties").exists() ||
    System.getenv("ANDROID_HOME") != null ||
    System.getenv("ANDROID_SDK_ROOT") != null

dependencyResolutionManagement {
    repositories {
        if (sdkFound) google()
        mavenCentral()
    }
}

rootProject.name = "Neumann"

include(":core")
if (sdkFound) {
    include(":app")
} else {
    println("No Android SDK found: building :core only")
}
