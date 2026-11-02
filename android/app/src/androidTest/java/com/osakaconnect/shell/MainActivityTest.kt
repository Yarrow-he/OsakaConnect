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
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun navigationRotationAndBackgroundReturnRecordLifecycle() {
        var originalId = ""
        var rotatedId = ""
        var targetOrientation = Configuration.ORIENTATION_UNDEFINED
        val observedEvents = linkedMapOf<String, MutableList<String>>()
        val foregroundChecks = mutableListOf<Result<Unit>>()
        val foregroundResumed = CountDownLatch(1)
        var returningFromHome = false
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val callback = ActivityLifecycleCallback { activity, stage ->
            if (activity is MainActivity) {
                val event = when (stage) {
                    Stage.CREATED -> "onCreate"
                    Stage.STARTED -> "onStart"
                    Stage.RESUMED -> "onResume"
                    Stage.PAUSED -> "onPause"
                    Stage.STOPPED -> "onStop"
                    Stage.DESTROYED -> "onDestroy"
                    else -> null
                }
                if (event != null) {
                    observedEvents.getOrPut(activity.lifecycleId) { mutableListOf() }.add(event)
                }
                if (returningFromHome && stage == Stage.RESUMED) {
                    // Check every resumed instance: HOME's orientation can trigger another recreation.
                    // Defer failures to the test thread; lifecycle listeners can swallow exceptions.
                    foregroundChecks.add(runCatching {
                        assertPage(activity, "P2")
                        activity.findViewById<Button>(R.id.navigate).performClick()
                        assertPage(activity, "P1")
                        // Leave P2 selected for any subsequent configuration-driven recreation.
                        activity.findViewById<Button>(R.id.navigate).performClick()
                        assertPage(activity, "P2")
                    })
                    foregroundResumed.countDown()
                }
            }
        }
        instrumentation.runOnMainSync { monitor.addLifecycleCallback(callback) }
        try {
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

                instrumentation.targetContext.startActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                await("background activity must stop") { "onStop" in lifecycleEvents(rotatedId) }
                instrumentation.runOnMainSync {
                    assertTrue("background onStop must be observed",
                        "onStop" in observedEvents.getValue(rotatedId))
                    returningFromHome = true
                }

                // Shell launch emulates returning from the launcher, including background-start rules.
                shell(
                    "am start -W -n ${instrumentation.targetContext.packageName}/" +
                        "${MainActivity::class.java.name} -f 0x10020000"
                )
                assertTrue("foreground activity must resume again",
                    foregroundResumed.await(10, TimeUnit.SECONDS))
            }
        } finally {
            instrumentation.runOnMainSync { monitor.removeLifecycleCallback(callback) }
        }

        assertTrue("return to foreground must check navigation", foregroundChecks.isNotEmpty())
        foregroundChecks.forEach { it.getOrThrow() }

        val fullLifecycle = listOf("onCreate", "onStart", "onResume", "onPause", "onStop", "onDestroy")
        await("all instance destructions must be logged") {
            observedEvents.keys.all { "onDestroy" in lifecycleEvents(it) }
        }
        assertEquals(fullLifecycle, lifecycleEvents(originalId))
        observedEvents.forEach { (id, events) ->
            assertEquals("$id must be created", "onCreate", events.first())
            assertEquals("$id must be destroyed", "onDestroy", events.last())
            assertEquals("$id must log every observed lifecycle callback", events, lifecycleEvents(id))
        }
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
