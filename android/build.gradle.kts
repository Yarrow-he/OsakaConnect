plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

// Toolchain: JDK 17, Gradle 8.9, Android SDK 35.
// Wrapper scripts/JAR are omitted under the task's file-type and binary restrictions.
// Once the toolchain and dependencies are available, run from android/:
// gradle --offline assembleDebug testDebugUnitTest lintDebug
// gradle --offline connectedDebugAndroidTest (requires an unlocked emulator/device)
// Reports: app/build/reports/tests/testDebugUnitTest/index.html,
// app/build/reports/lint-results-debug.html,
// app/build/reports/androidTests/connected/debug/index.html.
// These commands have NOT been run: this task prohibits building or downloading tools.
