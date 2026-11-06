package org.json

private const val MAX_NESTING_DEPTH = 64
private val NUMBER_FORMAT = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")

// shortcut: android.jar contains JVM stubs. This test-only, immutable API subset reads
// JSON without dependencies; it does not fully emulate Android's permissive parser.
// Use device tests if platform parsing behavior becomes part of the contract.
class JSONException(message: String) : RuntimeException(message)

class JSONObject(private val values: Map<String, Any>, private val source: String) {
    fun opt(name: String): Any? = values[name]
    fun get(name: String): Any = values[name] ?: throw JSONException("Missing field")
    fun keys(): Iterator<String> = values.keys.iterator()
    override fun toString(): String = source

    companion object {
        val NULL: Any = Any()
    }
}

class JSONArray(private val values: List<Any>) {
    fun length(): Int = values.size
    fun get(index: Int): Any = values[index]
}

class JSONTokener(private val source: String) {
    private var position = if (source.startsWith('\uFEFF')) 1 else 0

    fun nextClean(): Char {
        while (position < source.length && source[position] in " \t\r\n") position++
        return if (position < source.length) source[position++] else '\u0000'
    }

    fun nextValue(): Any = nextValue(0)

    private fun nextValue(depth: Int): Any {
        val first = nextClean()
        if (first == '{' || first == '[') checkJson(depth < MAX_NESTING_DEPTH)
        return when (first) {
            '{' -> readObject(position - 1, depth + 1)
            '[' -> readArray(depth + 1)
            '"' -> readString()
            't' -> literal("rue", true)
            'f' -> literal("alse", false)
            'n' -> literal("ull", JSONObject.NULL)
            '-', in '0'..'9' -> readNumber(position - 1)
            else -> throw JSONException("Expected JSON value")
        }
    }

    private fun readObject(start: Int, depth: Int): JSONObject {
        val values = linkedMapOf<String, Any>()
        if (!consume('}')) {
            do {
                expect('"')
                val key = readString()
                expect(':')
                values[key] = nextValue(depth)
            } while (consume(','))
            expect('}')
        }
        return JSONObject(values, source.substring(start, position))
    }

    private fun readArray(depth: Int): JSONArray {
        val values = mutableListOf<Any>()
        if (!consume(']')) {
            do {
                values.add(nextValue(depth))
            } while (consume(','))
            expect(']')
        }
        return JSONArray(values)
    }

    private fun readString(): String = buildString {
        while (position < source.length) {
            val char = source[position++]
            when {
                char == '"' -> return@buildString
                char == '\\' -> {
                    checkJson(position < source.length)
                    append(when (val escaped = source[position++]) {
                        '"', '\\', '/' -> escaped
                        'b' -> '\b'
                        'f' -> '\u000c'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'u' -> {
                            checkJson(position + 4 <= source.length)
                            val hex = source.substring(position, position + 4)
                            checkJson(hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
                            position += 4
                            hex.toInt(16).toChar()
                        }
                        else -> throw JSONException("Invalid escape")
                    })
                }
                else -> append(char)
            }
        }
        throw JSONException("Unterminated string")
    }

    private fun readNumber(start: Int): Number {
        while (position < source.length && source[position] !in " \t\r\n,]}") position++
        val number = source.substring(start, position)
        checkJson(NUMBER_FORMAT.matches(number))
        return if (number.any { it == '.' || it == 'e' || it == 'E' }) {
            number.toDouble()
        } else {
            number.toLongOrNull() ?: throw JSONException("Integer out of range")
        }
    }

    private fun literal(tail: String, value: Any): Any {
        checkJson(source.startsWith(tail, position))
        position += tail.length
        return value
    }

    private fun consume(char: Char): Boolean {
        val saved = position
        if (nextClean() == char) return true
        position = saved
        return false
    }

    private fun expect(char: Char) = checkJson(nextClean() == char)

    private fun checkJson(condition: Boolean) {
        if (!condition) throw JSONException("Invalid JSON")
    }
}
