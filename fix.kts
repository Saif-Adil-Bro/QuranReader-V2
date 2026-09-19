plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
    androidResources {
        noCompress += "db"
    }
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
