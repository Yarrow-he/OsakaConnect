package com.osakaconnect.diagnostics

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdminContractTest {
    @Test
    fun rejectsOversizedMessageField() {
        val input = """{"code":"INTERNAL","message":"${"x".repeat(1024 * 1024)}"}"""
        assertThrows(IllegalArgumentException::class.java) { AdminContract.parseError(input) }
    }

    @Test
    fun rejectsResponseOverSixtyFourKiB() {
        val body = """{"code":"INTERNAL","message":"短"}"""
        val input = body + " ".repeat(65537 - body.toByteArray(Charsets.UTF_8).size)
        assertEquals(65537, input.toByteArray(Charsets.UTF_8).size)
        val error = assertThrows(IllegalArgumentException::class.java) { AdminContract.parseError(input) }
        assertEquals("Response too large", error.message)
    }

    @Test
    fun acceptsResponseAtSixtyFourKiB() {
        val body = """{"code":"INTERNAL","message":"短"}"""
        val input = body + " ".repeat(65536 - body.toByteArray(Charsets.UTF_8).size)
        assertEquals(65536, input.toByteArray(Charsets.UTF_8).size)
        assertEquals(ErrorBody("INTERNAL", "短"), AdminContract.parseError(input))
    }

    @Test
    fun acceptsAndRejectsSharedContractVectors() {
        val vectors = SharedVectors.read("test-vectors/admin-contract.json")
        val parsers = mapOf<String, (String) -> Any>(
            "HealthResponse" to AdminContract::parseHealth,
            "VersionResponse" to AdminContract::parseVersion,
            "ErrorBody" to AdminContract::parseError
        )
        assertEquals(parsers.keys, vectors.keys().asSequence().filterNot { it.startsWith("_") }.toSet())
        parsers.forEach { (schema, parse) ->
            val cases = vectors.get(schema) as JSONObject
            cases.array("valid").forEach { value ->
                val body = value as JSONObject
                when (val parsed = parse(body.toString())) {
                    is HealthResponse -> {
                        assertEquals(body.get("status"), parsed.status)
                        val routes = body.get("routes") as JSONArray
                        assertEquals(routes.length(), parsed.routes.size)
                        parsed.routes.forEachIndexed { index, route ->
                            val expected = routes.get(index) as JSONObject
                            assertEquals(expected.get("id"), route.id)
                            assertEquals(expected.get("display_name"), route.displayName)
                        }
                    }
                    is VersionResponse -> {
                        assertEquals(body.get("service"), parsed.service)
                        assertEquals(body.get("version"), parsed.version)
                        assertEquals(body.get("protocol"), parsed.protocol)
                    }
                    is ErrorBody -> {
                        assertEquals(body.get("code"), parsed.code)
                        assertEquals(body.get("message"), parsed.message)
                    }
                    else -> error("Unexpected parsed model")
                }
            }
            cases.array("invalid").forEachIndexed { index, value ->
                assertThrows("$schema.invalid[$index]", IllegalArgumentException::class.java) {
                    parse(value.toString())
                }
            }
            listOf("", "null", "[]", "{", "{}", "{} {}").forEach { input ->
                assertThrows(IllegalArgumentException::class.java) { parse(input) }
            }
        }

        fun health(route: String) = """{"status":"degraded","routes":[$route]}"""
        fun route(id: String, name: String) = """{"id":"$id","display_name":"$name"}"""
        val unicodeName = "\uD83D\uDE80".repeat(64)
        val boundary = AdminContract.parseHealth(health(route("x".repeat(64), unicodeName)))
        assertEquals("degraded", boundary.status)
        assertEquals(unicodeName, boundary.routes.single().displayName)
        val twoRoutes = "${route("a", "a")},${route("b", "b")}"
        assertEquals(2, AdminContract.parseHealth(health(twoRoutes)).routes.size)

        listOf(
            route("x".repeat(65), "x"),
            route("x", "x".repeat(65)),
            route("x", ""),
            """{"id":123,"display_name":"x"}""",
            """{"id":"x","display_name":null}""",
            """{"id":"x","display_name":"x","extra":true}""",
            "null", "[]", "true"
        ).forEach { invalidRoute ->
            assertThrows(IllegalArgumentException::class.java) { AdminContract.parseHealth(health(invalidRoute)) }
        }
        listOf("null", "123", "true", "[]", "{}", "\"1.2.3\\n\"").forEach { version ->
            assertThrows(IllegalArgumentException::class.java) {
                AdminContract.parseVersion("""{"service":"osaka-server","version":$version,"protocol":"admin.v1"}""")
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            AdminContract.parseError("""{"code":"INTERNAL","message":"${"x".repeat(201)}"}""")
        }
        val validHealth = health(route("x", "x"))
        assertThrows(IllegalArgumentException::class.java) { AdminContract.parseHealth("$validHealth {}") }
        val error = assertThrows(IllegalArgumentException::class.java) {
            AdminContract.parseHealth("{FAKE_PRIVATE_VALUE")
        }
        assertEquals("Invalid response JSON", error.message)
    }
}
