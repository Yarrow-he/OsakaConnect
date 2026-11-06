package com.osakaconnect.diagnostics

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal object SharedVectors {
    fun read(path: String): JSONObject {
        val root = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "shared/test-vectors/admin-contract.json").isFile }
            ?: error("Run tests from the repository so shared/ can be found")
        val tokens = JSONTokener(File(root, "shared/$path").readText(Charsets.UTF_8))
        val result = tokens.nextValue() as JSONObject
        check(tokens.nextClean() == '\u0000')
        return result
    }
}

internal fun JSONObject.array(name: String): List<Any> {
    val array = get(name) as JSONArray
    return (0 until array.length()).map { array.get(it) }
}
