/*
 * 用途：android-diagnostics 分歧的仪器测试——在模拟器/真机上用平台 org.json 解析与
 *       json-divergence.json 相同的向量，断言解析成败与 JvmJson（JVM 单测源集替身）的 jvm.ok 一致；
 *       成功时按 jvm.value 字段（opt("code")/opt("v")/opt("a") 等）比较。
 * 前置环境：GitHub Actions ubuntu-latest + JDK 17 + Android SDK + 已启动模拟器（API >= 26），
 *           app 已配置 androidx.test（androidx.test.ext:junit、runner）；等组长的模拟器 CI 合入后才跑。
 * 运行命令：bash tests/android/android-diagnostics/run-instrumented.sh（内部 ./gradlew connectedDebugAndroidTest）。
 * 向量：内嵌下面的 cases，与 json-divergence.json 同步；改一处必须改两处。
 * 覆盖：A9/A7 相关（JvmJson 与真机分歧，审阅 2026-11-02）。
 */

package com.osakaconnect.diagnostics

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrgJsonDivergenceTest {

    private data class Case(
        val name: String,
        val input: String,
        val jvmOk: Boolean,
        val jvmValue: Map<String, Any?>? = null,
    )

    private val cases = listOf(
        Case("duplicate-keys", "{\"code\":\"INTERNAL\",\"code\":\"FORBIDDEN\",\"message\":\"x\"}", jvmOk = true, jvmValue = mapOf("code" to "FORBIDDEN")),
        Case("nan", "{\"v\":NaN}", jvmOk = false),
        Case("infinity", "{\"v\":Infinity}", jvmOk = false),
        Case("bare-tab", "{\"v\":\"a\tb\"}", jvmOk = true, jvmValue = mapOf("v" to "a\tb")),
        Case("bare-nul", "{\"v\":\"a" + 0x0000.toChar() + "b\"}", jvmOk = true, jvmValue = mapOf("v" to "a" + 0x0000.toChar() + "b")),
        Case("bom", 0xFEFF.toChar().toString() + "{\"a\":1}", jvmOk = true, jvmValue = mapOf("a" to 1)),
    )

    @Test
    fun deviceOrgJsonMatchesJvmExpectations() {
        val mismatches = mutableListOf<String>()
        for (case in cases) {
            var ok = true
            var value: Any? = null
            try {
                value = JSONTokener(case.input).nextValue()
            } catch (e: JSONException) {
                ok = false
            }

            if (case.jvmOk != ok) {
                mismatches += "${case.name}: 真机 org.json 解析成败与 JvmJson 不一致" +
                    "（input=${visible(case.input)}，真机结果=${if (ok) value else "JSONException"}，" +
                    "期望 ok=${case.jvmOk}）"
            }

            if (case.jvmOk && ok) {
                val expectedFields = case.jvmValue ?: continue
                val obj = value as? JSONObject
                if (obj == null) {
                    mismatches += "${case.name}: 真机解析成功但结果不是对象" +
                        "（input=${visible(case.input)}，真机结果=${visible(value.toString())}）"
                } else {
                    for ((key, expected) in expectedFields) {
                        val actual = obj.opt(key)
                        if (expected != actual) {
                            mismatches += "${case.name}: 字段 $key 与 JvmJson 值不一致" +
                                "（input=${visible(case.input)}，期望=${expected}，真机结果=${actual}）"
                        }
                    }
                }
            }
        }
        if (mismatches.isNotEmpty()) {
            fail("真机 org.json 与 JvmJson 期望不一致（${mismatches.size} 处）：\n" + mismatches.joinToString("\n"))
        }
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
