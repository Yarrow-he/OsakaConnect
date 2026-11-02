package com.osakaconnect.shell

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import android.os.SystemClock
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
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                originalId = activity.lifecycleId
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
                activity.requestedOrientation = if (
                    targetOrientation == Configuration.ORIENTATION_LANDSCAPE
                ) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            await("rotation must destroy the original activity") {
                "onDestroy" in lifecycleEvents(originalId)
            }
            await("rotated activity must resume") { scenario.state == Lifecycle.State.RESUMED }
            scenario.onActivity { activity ->
                rotatedId = activity.lifecycleId
                assertNotEquals(originalId, rotatedId)
                assertEquals(targetOrientation, activity.resources.configuration.orientation)
                assertPage(activity, "P2")
            }

            // Launch the real home screen so the app's task actually goes to the background.
            instrumentation.targetContext.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            await("background activity must stop") { "onStop" in lifecycleEvents(rotatedId) }
            assertEquals(Lifecycle.State.CREATED, scenario.state)

            // Shell launch emulates returning from the launcher, including background-start rules.
            shell(
                "am start -W -n ${instrumentation.targetContext.packageName}/" +
                    "${MainActivity::class.java.name} -f 0x10020000"
            )
            await("foreground activity must resume again") {
                lifecycleEvents(rotatedId).count { it == "onResume" } == 2
            }
            scenario.onActivity { activity ->
                assertEquals(rotatedId, activity.lifecycleId)
                assertPage(activity, "P2")
                activity.findViewById<Button>(R.id.navigate).performClick()
                assertPage(activity, "P1")
            }
        }

        val fullLifecycle = listOf("onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy")
        await("final destruction must be logged") { "onDestroy" in lifecycleEvents(rotatedId) }
        assertEquals(fullLifecycle, lifecycleEvents(originalId))
        assertEquals(
            listOf("onCreate", "onStart", "onResume", "onPause", "onStop", "onStart", "onResume",
                "onPause", "onStop", "onDestroy"),
            lifecycleEvents(rotatedId)
        )
    }

    private fun assertPage(activity: MainActivity, expected: String) {
        assertEquals(expected, activity.findViewById<TextView>(R.id.page_identifier).text.toString())
    }

    private fun lifecycleEvents(id: String): List<String> =
        shell("logcat -d -v raw -s OsakaLifecycle:I *:S").lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("$id ") }
            .map { it.removePrefix("$id ") }
            .toList()

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
