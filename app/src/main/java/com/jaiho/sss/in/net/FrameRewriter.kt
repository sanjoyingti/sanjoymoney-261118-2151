package com.jaiho.sss.`in`.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Rewrites a WebSocket text frame:
 *  - objects that mention result/win/prize/reward/jp/jackpot get "code" pushed to at least 200
 *  - numeric win/prize/reward/jackpot fields are multiplied
 *  - jackpot style booleans become true
 * Returns null when the frame could not be parsed or nothing changed (the caller then
 * passes the original bytes through untouched).
 */
object FrameRewriter {

    private val TRIGGERS = listOf("result", "win", "prize", "reward", "jp", "jackpot")
    private val NUMERIC = listOf("win", "prize", "reward", "jackpot", "jp")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun matches(key: String): Boolean {
        val lower = key.lowercase()
        return TRIGGERS.any { lower.contains(it) }
    }

    private fun numericField(key: String): Boolean {
        val lower = key.lowercase()
        return NUMERIC.any { lower.contains(it) }
    }

    fun rewrite(text: String, multiplier: Float): String? {
        return try {
            val root = json.parseToJsonElement(text)
            var changed = false
            val rewritten = walk(root, multiplier) { changed = true }
            if (!changed) null else Json.encodeToString(JsonElement.serializer(), rewritten)
        } catch (t: Throwable) {
            null
        }
    }

    private fun walk(element: JsonElement, multiplier: Float, mark: () -> Unit): JsonElement {
        return when (element) {
            is JsonObject -> {
                val hasTrigger = element.keys.any { matches(it) }
                val rebuilt = LinkedHashMap<String, JsonElement>()
                for ((key, value) in element) {
                    val lower = key.lowercase()
                    if (hasTrigger && lower == "code" && value is JsonPrimitive) {
                        val asLong = value.content.toLongOrNull()
                        if (asLong != null && asLong < 200) {
                            rebuilt[key] = JsonPrimitive(200)
                            mark()
                            continue
                        }
                    }
                    if (matches(key) && value is JsonPrimitive) {
                        val bool = value.content.toBooleanStrictOrNull()
                        if (lower != "code" && !value.isString && bool != null) {
                            if (!bool) {
                                rebuilt[key] = JsonPrimitive(true)
                                mark()
                            } else {
                                rebuilt[key] = value
                            }
                            continue
                        }
                        if (numericField(key) && !value.isString) {
                            val asDouble = value.content.toDoubleOrNull()
                            if (asDouble != null) {
                                val scaled = (asDouble * multiplier).toLong()
                                if (scaled != asDouble.toLong()) {
                                    rebuilt[key] = if (value.content.contains('.')) {
                                        JsonPrimitive(asDouble * multiplier)
                                    } else {
                                        JsonPrimitive(scaled)
                                    }
                                    mark()
                                } else {
                                    rebuilt[key] = value
                                }
                                continue
                            }
                        }
                    }
                    rebuilt[key] = walk(value, multiplier, mark)
                }
                JsonObject(rebuilt)
            }
            is JsonArray -> JsonArray(element.map { walk(it, multiplier, mark) })
            else -> element
        }
    }
}
