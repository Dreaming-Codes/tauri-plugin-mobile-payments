# Tauri Plugin mobile-payments

Init this like any other tauri plugin
and this to your kotlin compiler args "-Xskip-metadata-version-check"
```kotlin
// src-tauri/gen/android/app/build.gradle.kts
kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs += "-Xskip-metadata-version-check"
}
```
