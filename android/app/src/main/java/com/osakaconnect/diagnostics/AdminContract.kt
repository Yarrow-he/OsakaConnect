package com.osakaconnect.diagnostics

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

data class Route(val id: String, val displayName: String)
data class HealthResponse(val status: String, val routes: List<Route>)
data class VersionResponse(val service: String, val version: String, val protocol: String)
data class ErrorBody(val code: String, val message: String)

object AdminContract {
    private const val MAX_RESPONSE_BYTES = 64 * 1024
    private val versionFormat = Regex("[0-9]+\\.[0-9]+\\.[0-9]+")

    fun parseHealth(json: String): HealthResponse {
        val body = parse(json)
        body.fields("status", "routes")
        val status = body.string("status")
        require(status == "ok" || status == "degraded") { "Invalid health status" }
        val routes = body.opt("routes") as? JSONArray
            ?: throw IllegalArgumentException("Invalid routes")
        require(routes.length() >= 1) { "Empty routes" }
        return HealthResponse(status, (0 until routes.length()).map { index ->
            val route = routes.get(index) as? JSONObject
                ?: throw IllegalArgumentException("Invalid route")
            route.fields("id", "display_name")
            Route(route.string("id", 1, 64), route.string("display_name", 1, 64))
        })
    }

    fun parseVersion(json: String): VersionResponse {
        val body = parse(json)
        body.fields("service", "version", "protocol")
        val service = body.string("service")
        val version = body.string("version")
        val protocol = body.string("protocol")
        require(service == "osaka-server" && protocol == "admin.v1") { "Invalid protocol" }
        require(versionFormat.matches(version)) { "Invalid version" }
        return VersionResponse(service, version, protocol)
    }

    fun parseError(json: String): ErrorBody {
        val body = parse(json)
        body.fields("code", "message")
        val code = body.string("code")
        require(ErrorMessages.isKnown(code)) { "Invalid error code" }
        return ErrorBody(code, body.string("message", 0, 200))
    }

    private fun parse(json: String): JSONObject {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_RESPONSE_BYTES) { "Response too large" }
        try {
            val tokens = JSONTokener(json)
            val body = tokens.nextValue() as? JSONObject
                ?: throw IllegalArgumentException("Expected response object")
            require(tokens.nextClean() == '\u0000') { "Trailing response content" }
            return body
        } catch (_: JSONException) {
            // Do not propagate parser messages that may contain response data.
            throw IllegalArgumentException("Invalid response JSON")
        }
    }

    private fun JSONObject.fields(vararg expected: String) {
        require(keys().asSequence().toSet() == expected.toSet()) { "Invalid response fields" }
    }

    private fun JSONObject.string(name: String, min: Int = 0, max: Int = Int.MAX_VALUE): String {
        // getString coerces non-strings on Android; schema validation must not.
        val value = opt(name) as? String ?: throw IllegalArgumentException("Invalid string field")
        require(value.codePointCount(0, value.length) in min..max) { "Invalid string length" }
        return value
    }
}
