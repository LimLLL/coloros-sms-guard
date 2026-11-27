plugins {
    id("com.android.application")
}

// CI injects the release key through env vars; without them the release APK stays unsigned.
val keystorePath: String? = System.getenv("SIGNING_KEYSTORE_PATH")

android {
    namespace = "io.github.limlll.smsguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.limlll.smsguard"
        minSdk = 29
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 would strip the hook entry class, which is only referenced from assets/xposed_init.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(project(":xposed-stub"))
    testImplementation("junit:junit:4.13.2")
}
