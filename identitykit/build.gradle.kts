plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 35
    namespace = "com.onesoftware.identitykit"

    defaultConfig {
        minSdk = 21
        buildConfigField("int", "VERSION_CODE", "43")
        buildConfigField("String", "VERSION_NAME", "\"1.0.2\"")

        // FIX: Updated to AndroidX (Support library is deprecated and incompatible with SDK 35)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        targetSdk = 35
    }

    lint {
        targetSdk = 35
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        // FIX: Ensure your project is using Java 17 in Gradle settings
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("com.android.volley:volley:1.2.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.12.1")
}

// FIX: Correct KTS syntax for configuring tasks by type
tasks.withType<Javadoc>().configureEach {
    isEnabled = false
}