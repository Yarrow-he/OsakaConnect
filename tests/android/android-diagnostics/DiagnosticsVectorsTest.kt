/*
 * 用途：android-diagnostics JVM 单测草稿——LogRedactor 跑 shared/test-vectors/redaction.json 全部 case（A7，与 Windows 同一份向量）；
 *       InviteCode.isValid 跑 invite-code.json 全部正反例（A15）；admin-contract.json 解析校验函数正反例（0.2.0 预检）。
 * 前置环境：ubuntu-latest + JDK 17 + Android SDK；仓库根含 shared/test-vectors/；产品 android/ 已交付。
 * 运行命令（待 CI；本机无 JDK/SDK，不跑构建）：bash tests/android/android-diagnostics/run.sh（内部 ./gradlew testDebugUnitTest）。
 * 覆盖 acceptance：A7、A15；admin-contract 为 notes/android-diagnostics.md 所列 0.2.0 预检项。
 * 待 CI：本文件只作草稿，未在本机编译。
 */

package com.osakaconnect.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNoException
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

// 产品入口：类名来自 brief；包名、方法名与返回约定待写手交付后对齐。
private const val PRODUCT_PACKAGE = "com.osakaconnect.diagnostics" // 待写手交付后对齐
private val LOG_REDACTOR_CLASS = "$PRODUCT_PACKAGE.LogRedactor" // 待对齐；约定 (String) -> String
private const val LOG_REDACT_METHOD = "redact" // 待对齐
private val INVITE_CODE_CLASS = "$PRODUCT_PACKAGE.InviteCode" // 待对齐
private const val INVITE_IS_VALID_METHOD = "isValid" // 待对齐；约定 (String?) -> Boolean
// 来源：android/android-diagnostics b691b21 AdminContract.kt:16/32/43（object 无 validate，按 schema 分方法，非法抛异常）
private val ADMIN_CONTRACT_CLASS = "$PRODUCT_PACKAGE.AdminContract"
private val ADMIN_SCHEMA_METHODS = mapOf(
    "HealthResponse" to "parseHealth",
    "VersionResponse" to "parseVersion",
    "ErrorBody" to "parseError",
)

class DiagnosticsVectorsTest {

    @Test
    fun logRedactorMatchesAllRedactionVectors() {
        val redact = entry(LOG_REDACTOR_CLASS, LOG_REDACT_METHOD, 1)
        val cases = ((vector("redaction.json") as Map<*, *>)["cases"] as List<*>)
        assertTrue("redaction.json 至少要有 1 个 case", cases.isNotEmpty())
        for (case in cases) {
            val c = case as Map<*, *>
            val input = c["in"] as String
            val expected = c["out"] as String
            assertEquals("LogRedactor 脱敏结果不符：$input", expected, redact.invoke(input))
        }
    }

    @Test
    fun inviteCodeMatchesAllVectors() {
        val isValid = entry(INVITE_CODE_CLASS, INVITE_IS_VALID_METHOD, 1)
        val root = vector("invite-code.json") as Map<*, *>
        val valid = root["valid"] as List<*>
        val invalid = root["invalid"] as List<*>
        assertTrue("invite-code.json 缺正例", valid.isNotEmpty())
        assertTrue("invite-code.json 缺反例", invalid.isNotEmpty())
        for (code in valid) assertEquals("合法邀请码被拒：$code", true, isValid.invoke(code))
        for (code in invalid) assertEquals("非法邀请码被收：$code", false, isValid.invoke(code))
    }

    @Test
    fun adminContractRejectsInvalidAndAcceptsValid() {
        val root = vector("admin-contract.json") as Map<*, *>
        for ((schema, methodName) in ADMIN_SCHEMA_METHODS) {
            val accept = entry(ADMIN_CONTRACT_CLASS, methodName, 1)
            val block = root[schema] as? Map<*, *> ?: error("admin-contract.json 缺 $schema")
            for (raw in block["valid"] as List<*>) {
                val json = Json.stringify(raw)
                assertTrue("$schema 合法样例被拒：$json", isAccepted(accept, json))
            }
            for (raw in block["invalid"] as List<*>) {
                val json = Json.stringify(raw)
                assertFalse("$schema 非法样例被收：$json", isAccepted(accept, json))
            }
        }
    }
}

// 解析函数：不抛异常且返回非空=收；抛异常=拒。
private fun isAccepted(parse: Entry, rawJson: String): Boolean =
    try {
        parse.invoke(rawJson) != null
    } catch (e: Exception) {
        false
    }

private class Entry(private val method: Method, private val owner: Any?) {
    fun invoke(vararg args: Any?): Any? =
        try {
            method.invoke(owner, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
}

private fun entry(className: String, methodName: String, arity: Int): Entry {
    val cls: Class<*> = try {
        Class.forName(className)
    } catch (e: ClassNotFoundException) {
        println("待测：模块未交付（$className）")
        assumeNoException("待测：模块未交付（$className）", e)
        error("unreachable")
    }
    val method = cls.methods.firstOrNull { it.name == methodName && it.parameterCount == arity }
        ?: error("待对齐：找不到 $className.$methodName/$arity")
    val owner: Any? = if (Modifier.isStatic(method.modifiers)) null else {
        val field = cls.declaredFields.firstOrNull { it.name == "INSTANCE" || it.name == "Companion" }
            ?: error("待对齐：$className.$methodName 非 static/object/companion")
        field.isAccessible = true
        field.get(null)
    }
    return Entry(method, owner)
}

// 从 user.dir 向上找仓库根，按仓库根相对路径读向量。
private fun vector(name: String): Any? {
    var dir: File? = File(System.getProperty("user.dir")).absoluteFile
    while (dir != null) {
        val file = File(dir, "shared/test-vectors/$name")
        if (file.isFile) return Json.parse(file.readText())
        dir = dir.parentFile
    }
    error("找不到 shared/test-vectors/$name（从 ${System.getProperty("user.dir")} 向上查找）")
}

// 不新增 JUnit 以外依赖：向量读取/回写用下面的最小 JSON 解析器。
private object Json {
    fun parse(text: String): Any? {
        val p = Parser(text)
        val value = p.value()
        p.skipWs()
        require(p.done()) { "JSON 尾部有多余内容" }
        return value
    }

    fun stringify(value: Any?): String = when (value) {
        null -> "null"
        is String -> buildString {
            append('"')
            for (c in value) when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
            append('"')
        }
        is Map<*, *> -> value.entries.joinToString(",", "{", "}") {
            stringify(it.key) + ":" + stringify(it.value)
        }
        is List<*> -> value.joinToString(",", "[", "]") { stringify(it) }
        else -> value.toString() // 向量里除字符串外只有布尔/null/数字
    }
}

private class Parser(private val s: String) {
    private var i = 0

    fun done() = i >= s.length

    fun skipWs() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    fun value(): Any? {
        skipWs()
        require(i < s.length) { "JSON 意外结束" }
        return when (s[i]) {
            '{' -> obj()
            '[' -> arr()
            '"' -> str()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> num()
        }
    }

    private fun obj(): Map<String, Any?> {
        i++ // {
        val out = LinkedHashMap<String, Any?>()
        skipWs()
        if (peek('}')) { i++; return out }
        while (true) {
            skipWs()
            val key = str()
            skipWs()
            require(peek(':')) { "JSON 缺少 :" }
            i++
            out[key] = value()
            skipWs()
            when {
                peek(',') -> i++
                peek('}') -> { i++; return out }
                else -> error("JSON 对象缺少 , 或 }")
            }
        }
    }

    private fun arr(): List<Any?> {
        i++ // [
        val out = ArrayList<Any?>()
        skipWs()
        if (peek(']')) { i++; return out }
        while (true) {
            out.add(value())
            skipWs()
            when {
                peek(',') -> i++
                peek(']') -> { i++; return out }
                else -> error("JSON 数组缺少 , 或 ]")
            }
        }
    }

    private fun str(): String {
        require(peek('"')) { "JSON 期望字符串" }
        i++
        val out = StringBuilder()
        while (true) {
            require(i < s.length) { "JSON 字符串未闭合" }
            when (val c = s[i++]) {
                '"' -> return out.toString()
                '\\' -> {
                    require(i < s.length) { "JSON 转义未闭合" }
                    when (val e = s[i++]) {
                        '"', '\\', '/' -> out.append(e)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            require(i + 4 <= s.length) { "JSON \\u 转义不完整" }
                            out.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> error("JSON 非法转义 \\$e")
                    }
                }
                else -> out.append(c)
            }
        }
    }

    private fun num(): Any {
        val start = i
        if (peek('-')) i++
        while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
        val raw = s.substring(start, i)
        require(raw.isNotEmpty()) { "JSON 非法值" }
        return raw.toLongOrNull() ?: raw.toDouble()
    }

    private fun literal(word: String, result: Any?): Any? {
        require(s.startsWith(word, i)) { "JSON 非法字面量" }
        i += word.length
        return result
    }

    private fun peek(c: Char) = i < s.length && s[i] == c
}
