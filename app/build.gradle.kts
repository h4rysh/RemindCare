plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
  alias(libs.plugins.kover)
}
android {
  namespace = "com.aistudio.remindcare"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  defaultConfig {
    applicationId = "com.aistudio.remindcare.pqyzwt"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0.1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }
  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all {
        it.forkEvery = 1
      }
    }
  }
}
ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
  implementation("com.google.firebase:firebase-auth")
  implementation(libs.firebase.crashlytics)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.androidx.work.runtime.ktx)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.truth)
  testImplementation(libs.androidx.arch.core.testing)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.androidx.room.compiler)
}

kover {
  reports {
    filters {
      includes {
        classes(
          "com.aistudio.remindcare.domain.usecase.*",
          "com.aistudio.remindcare.data.mapper.*"
        )
      }
    }
    verify {
      rule("Domain and Data Mapper Minimum 80% Coverage") {
        minBound(80)
      }
    }
  }
}

tasks.register("koverVerifyDebug") {
  group = "verification"
  description = "Alias for koverVerify on debug build"
  dependsOn("koverVerify")
}

tasks.register("koverHtmlReportDebug") {
  group = "verification"
  description = "Alias for koverHtmlReport on debug build"
  dependsOn("koverHtmlReport")
}
