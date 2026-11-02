/*
 * 用途：android-shell 仪器测试草稿（Espresso + ActivityScenario）——未激活 P1 / 已激活 P2
 *       两页切换并断言页面标识（A16）；旋转、后台→前台不崩溃且生命周期回调被记录（A6）。
 * 前置环境：写手交付后把本文件放入 app/src/androidTest/kotlin/ 对应包路径；Gradle 已配置
 *           androidx.test（espresso-core、androidx.test.ext:junit、androidx.test:core/runner）；
 *           模拟器/真机 API >= 26。本机不装 JDK/SDK，不运行；CI 用 ubuntu-latest + 模拟器执行。
 * 运行命令：cd android && ./gradlew connectedDebugAndroidTest
 * 覆盖 acceptance：A6、A16；A5 的启动无崩溃由 ci-run.sh 覆盖，本测试启动 Activity 亦间接覆盖。
 * 待写手交付后对齐：package 行、APP_ID、MAIN_ACTIVITY、PAGE_*、NAV_*、LIFECYCLE_TAG 与页面标识暴露方式。
 */
package com.osaka.connect // 待写手交付后对齐

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

// 待写手交付后对齐：取自 design/inventory.md（P1=未激活，P2=已激活）与 android/ 实际实现。
private const val APP_ID = "com.osaka.connect" // TODO
private const val MAIN_ACTIVITY = "$APP_ID.MainActivity" // TODO
private const val PAGE_P1 = "P1" // 未激活页标识
private const val PAGE_P2 = "P2" // 已激活页标识
private const val NAV_TO_P1 = "P1" // 切到未激活页的控件标识
private const val NAV_TO_P2 = "P2" // 切到已激活页的控件标识
private const val LIFECYCLE_TAG = "OsakaLifecycle" // 生命周期日志 tag，TODO

@RunWith(AndroidJUnit4::class)
class ShellNavigationTest {

    private fun launch(): ActivityScenario<Activity> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(APP_ID, MAIN_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    // 页面标识若以 tag/testTag 暴露，只改这一处。
    private fun assertPageVisible(id: String) {
        onView(withContentDescription(id)).check(matches(isDisplayed()))
    }

    private fun clearLogcat() {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("logcat -c")
        FileInputStream(pfd.fileDescriptor).close()
    }

    private fun lifecycleLog(): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("logcat -d -s $LIFECYCLE_TAG")
        return FileInputStream(pfd.fileDescriptor).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun count(log: String, token: String): Int =
        Regex(Regex.escape(token), RegexOption.IGNORE_CASE).findAll(log).count()

    private fun waitForLog(token: String, atLeast: Int, timeoutMs: Long = 5000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (count(lifecycleLog(), token) >= atLeast) return
            Thread.sleep(100)
        }
        val log = lifecycleLog()
        assertTrue("生命周期日志缺少 $token（≥$atLeast）：\n$log", count(log, token) >= atLeast)
    }

    private fun awaitIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(500) // 等生命周期日志落盘
    }

    @Test
    fun pageSwitchShowsP1AndP2Identifiers() {
        val scenario = launch()
        try {
            // 启动默认进入未激活 P1。
            assertPageVisible(PAGE_P1)
            onView(withContentDescription(NAV_TO_P2)).perform(click())
            awaitIdle()
            assertPageVisible(PAGE_P2)
            onView(withContentDescription(NAV_TO_P1)).perform(click())
            awaitIdle()
            assertPageVisible(PAGE_P1)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun rotationRecordsLifecycleAndDoesNotCrash() {
        clearLogcat()
        val scenario = launch()
        try {
            waitForLog("onCreate", 1)
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            waitForLog("onCreate", 2) // 横屏重建，onCreate 至少记录两次
            awaitIdle()
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            waitForLog("onCreate", 3) // 竖屏再次重建
            assertPageVisible(PAGE_P1)
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundThenForegroundRecordsLifecycleAndDoesNotCrash() {
        clearLogcat()
        val scenario = launch()
        try {
            waitForLog("onResume", 1)
            scenario.moveToState(Lifecycle.State.CREATED) // 后台：触发 onPause/onStop
            waitForLog("onPause", 1)
            scenario.moveToState(Lifecycle.State.RESUMED) // 回前台：触发 onResume
            waitForLog("onResume", 2)
            assertPageVisible(PAGE_P1)
        } finally {
            scenario.close()
        }
    }
}
