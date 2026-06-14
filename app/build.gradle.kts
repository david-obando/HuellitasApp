plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.huellitas.huellitasapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.huellitas.huellitasapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Supabase Auth y base de datos
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.5.0") // Auth
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.0") // Database
    implementation("io.github.jan-tennert.supabase:storage-kt:2.5.0") // Subir imagenes a Supabase
    implementation("io.ktor:ktor-client-android:2.3.10") // Motor para las peticiones
    implementation("io.coil-kt:coil:2.6.0")//imagenes del recyclerview
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") //el motor de serialización funcione
    implementation("com.google.android.gms:play-services-maps:18.2.0") //gps mapa
    implementation("com.google.android.gms:play-services-location:21.3.0") // gps ubicacion
}