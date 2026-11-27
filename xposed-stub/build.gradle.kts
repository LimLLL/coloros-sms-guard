// Compile-only stand-ins for the legacy Xposed API (api:82). LSPosed provides the real
// classes at runtime, so these must never be packaged into the APK.
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
