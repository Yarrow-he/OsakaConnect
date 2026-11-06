package com.osakaconnect.shell

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun navigationRotationAndBackgroundReturnRecordLifecycle() {
        var originalId = ""
        var rotatedId = ""
        var targetOrientation = Configuration.ORIENTATION_UNDEFINED
        var targetRotation = 0
        val accelerometerRotation = shell("settings get system accelerometer_rotation").trim()
        val userRotation = shell("settings get system user_rotation").trim()
        val previousIds = lifecycleLog().map { it.substringBefore(' ') }.toSet()

        fun logEvidence(stage: String, activity: MainActivity? = null) {
            Log.i("OsakaLifecycleTest", "$stage: saved accelerometer_rotation=$accelerometerRotation " +
                "user_rotation=$userRotation; targetRotation=$targetRotation " +
                "targetOrientation=$targetOrientation; current accelerometer_rotation=" +
                shell("settings get system accelerometer_rotation").trim() +
                " user_rotation=" + shell("settings get system user_rotation").trim() +
                " configuration.orientation=" +
                (activity?.resources ?: instrumentation.targetContext.resources).configuration.orientation +
                " activityId=${activity?.lifecycleId} originalId=$originalId rotatedId=$rotatedId")
            lifecycleLog().filter { it.substringBefore(' ') !in previousIds }.forEach {
                Log.i("OsakaLifecycleTest", "$stage: $it")
            }
        }

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    originalId = activity.lifecycleId
                    logEvidence("initial activity", activity)
                    assertPage(activity, "P1")
                    activity.findViewById<Button>(R.id.navigate).performClick()
                    assertPage(activity, "P2")
                    activity.findViewById<Button>(R.id.navigate).performClick()
                    assertPage(activity, "P1")
                    activity.findViewById<Button>(R.id.navigate).performClick()
                    assertPage(activity, "P2")

                    targetOrientation = if (
                        activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    ) Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                    targetRotation = if (activity.windowManager.defaultDisplay.rotation % 2 == 0) 1 else 0
                    logEvidence("before locking display rotation", activity)
                }
                // Set the target first so disabling the sensor cannot apply a stale user rotation.
                shell("settings put system user_rotation $targetRotation")
                shell("settings put system accelerometer_rotation 0")
                logEvidence("display rotation locked")

                await("rotation must destroy the original activity") {
                    "onDestroy" in lifecycleEvents(originalId)
                }
                await("rotated activity must resume") { scenario.state == Lifecycle.State.RESUMED }
                scenario.onActivity { activity ->
                    rotatedId = activity.lifecycleId
                    logEvidence("rotated activity", activity)
                    assertNotEquals(originalId, rotatedId)
                    assertEquals(targetOrientation, activity.resources.configuration.orientation)
                    assertPage(activity, "P2")
                    // Keep HOME's orientation from causing a configuration change on return.
                    activity.requestedOrientation = if (targetOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
                instrumentation.waitForIdleSync()
                await("orientation-locked activity must resume in the target orientation") {
                    var ready = false
                    if (scenario.state == Lifecycle.State.RESUMED) {
                        scenario.onActivity { activity ->
                            ready = !activity.isChangingConfigurations &&
                                activity.resources.configuration.orientation == targetOrientation
                            if (ready) {
                                assertPage(activity, "P2")
                                rotatedId = activity.lifecycleId
                                logEvidence("orientation-locked activity before HOME", activity)
                            }
                        }
                    }
                    ready
                }

                // Launch the real home screen so the app's task actually goes to the background.
                instrumentation.targetContext.startActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                await("background activity must stop") { "onStop" in lifecycleEvents(rotatedId) }
                logEvidence("background activity")
                assertEquals(Lifecycle.State.CREATED, scenario.state)
                assertTrue("onDestroy" !in lifecycleEvents(rotatedId))

                shell(
                    "am start -W -n ${instrumentation.targetContext.packageName}/" +
                        "${MainActivity::class.java.name} -f 0x10020000"
                )
                await("foreground activity must resume again") {
                    lifecycleEvents(rotatedId).count { it == "onResume" } == 2
                }
                await("foreground activity must retain its id and target orientation") {
                    var ready = false
                    scenario.onActivity { activity ->
                        ready = activity.resources.configuration.orientation == targetOrientation &&
                            activity.lifecycleId == rotatedId
                    }
                    ready
                }
                scenario.onActivity { activity ->
                    logEvidence("foreground activity", activity)
                    assertEquals(rotatedId, activity.lifecycleId)
                    assertPage(activity, "P2")
                    activity.findViewById<Button>(R.id.navigate).performClick()
                    assertPage(activity, "P1")
                }
            }

            val fullLifecycle = listOf("onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy")
            await("final destruction must be logged") { "onDestroy" in lifecycleEvents(rotatedId) }
            logEvidence("final lifecycle assertions")
            assertEquals(fullLifecycle, lifecycleEvents(originalId))
            assertEquals(
                listOf("onCreate", "onStart", "onResume", "onPause", "onStop", "onStart", "onResume",
                    "onPause", "onStop", "onDestroy"),
                lifecycleEvents(rotatedId)
            )
            val testIds = lifecycleLog().map { it.substringBefore(' ') }.toSet() - previousIds
            Log.i("OsakaLifecycleTest", "test lifecycleIds=$testIds count=${testIds.size}")
            assertTrue("at most 3 activity instances: $testIds", testIds.size <= 3)
        } catch (failure: AssertionError) {
            logEvidence("assertion failed: ${failure.message}")
            throw failure
        } finally {
            try {
                restoreSetting("user_rotation", userRotation)
            } finally {
                restoreSetting("accelerometer_rotation", accelerometerRotation)
            }
        }
    }

    private fun assertPage(activity: MainActivity, expected: String) {
        assertEquals(expected, activity.findViewById<TextView>(R.id.page_identifier).text.toString())
    }

    private fun lifecycleLog(): List<String> =
        shell("logcat -d -v raw -s OsakaLifecycle:I *:S").lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("---------") }
            .toList()

    private fun lifecycleEvents(id: String): List<String> =
        lifecycleLog()
            .filter { it.startsWith("$id ") }
            .map { it.removePrefix("$id ") }
            .toList()

    private fun restoreSetting(name: String, value: String) {
        if (value.isEmpty() || value == "null") {
            shell("settings delete system $name")
        } else {
            shell("settings put system $name '${value.replace("'", "'\\''")}'")
        }
    }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            assertTrue(message, SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(100)
        }
    }
}
