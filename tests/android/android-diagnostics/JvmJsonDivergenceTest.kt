/*
 * 用途：用 JVM 单测源集里的 org.json 替身（JvmJson，/tmp/JvmJson-97d42d6.kt @97d42d6）解析
 *       json-divergence.json 的全部向量，断言解析成败与向量 jvm.ok 一致；成功时按 jvm.value 字段比较。
 * 前置环境：ubuntu-latest + JDK 17 + Android SDK；仓库根含 android/app 与 tests/android/android-diagnostics/。
 *           JvmJson 位于 android/app/src/test（package org.json），会遮蔽 android.jar 的同名 org.json，
 *           因此它不能与真机 org.json 同 classpath 比较；真机侧见 androidTest/OrgJsonDivergenceTest.kt。
 * 运行命令（本机不跑）：bash tests/android/android-diagnostics/run-instrumented.sh（内部 ./gradlew testDebugUnitTest）。
 * 覆盖：A9/A7 相关（JvmJson 与真机分歧，审阅 2026-11-02）。
 */

package com.osakaconnect.diagnostics

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JvmJsonDivergenceTest {

    @Test
    fun jvmJsonMatchesVectorExpectations() {
        val cases = vector().opt("cases") as JSONArray
        assertTrue("json-divergence.json 至少要有 1 个 case", cases.length() > 0)
        for (i in 0 until cases.length()) {
            val case = cases.get(i) as JSONObject
            val name = case.opt("name") as String
            val input = case.opt("input") as String
            val jvm = case.opt("jvm") as JSONObject
            val expectedOk = jvm.opt("ok") as Boolean

            var ok = true
            var value: Any? = null
            try {
                value = JSONTokener(input).nextValue()
            } catch (e: JSONException) {
                ok = false
            }

            assertEquals("$name: JvmJson 解析成败与向量 jvm.ok 不一致（input=${visible(input)}）", expectedOk, ok)
            if (expectedOk && ok) {
                val expectedValue = jvm.opt("value") as? JSONObject ?: continue
                val obj = value as? JSONObject ?: error("$name: JvmJson 成功但结果不是对象")
                val keys = expectedValue.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    assertEquals("$name: 字段 $key 与向量 jvm.value 不一致", expectedValue.opt(key), obj.opt(key))
                }
            }
        }
    }

    // 向量文件在仓库 tests/ 下；Gradle 单测工作目录为 android/app，向上找到仓库根后按相对路径读取。
    private fun vector(): JSONObject {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val file = File(dir, "tests/android/android-diagnostics/json-divergence.json")
            if (file.isFile) return JSONTokener(file.readText()).nextValue() as JSONObject
            dir = dir.parentFile
        }
        error("找不到 tests/android/android-diagnostics/json-divergence.json（从 ${System.getProperty("user.dir")} 向上查找）")
    }

    private fun visible(s: String): String = buildString {
        for (c in s) when {
            '\t' == c -> append("\\t")
            '\n' == c -> append("\\n")
            '\r' == c -> append("\\r")
            c < ' ' -> append("\\u%04x".format(c.code))
            c.code == 0xFEFF -> append("\\uFEFF")
            else -> append(c)
        }
    }
}
