/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("TooManyFunctions", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.config.gson.util

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import net.ccbluex.liquidbounce.config.gson.publicGson
import org.apache.commons.io.input.CharSequenceReader
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset

/**
 * Read JSON content
 */
inline fun <reified T> CharSequence.readJson(): T =
    CharSequenceReader(this).readJson()

/**
 * Read JSON content from a [File].
 */
inline fun <reified T> File.readJson(charset: Charset = Charsets.UTF_8): T =
    inputStream().readJson(charset)

/**
 * Read JSON content from an [InputStream] and close it
 */
inline fun <reified T> InputStream.readJson(charset: Charset = Charsets.UTF_8): T =
    bufferedReader(charset).readJson()

/**
 * Read JSON content from a [Reader] and close it
 */
inline fun <reified T> Reader.readJson(gson: Gson = publicGson): T = use {
    gson.fromJson(it, object : TypeToken<T>() {}.type)
}

inline fun JsonReader.parseTree(): JsonElement = JsonParser.parseReader(this)

inline fun <reified T> JsonDeserializationContext.deserialize(json: JsonElement): T =
    deserialize(json, object : TypeToken<T>() {}.type)

@DslMarker
annotation class JsonDsl

@JsonDsl
@JvmInline
value class JsonArrayBuilder(private val backend: JsonArray) {

    constructor(initialCapacity: Int) : this(JsonArray(initialCapacity))

    operator fun JsonElement?.unaryPlus() {
        backend.add(this)
    }

    operator fun Boolean?.unaryPlus() {
        backend.add(this)
    }

    operator fun String?.unaryPlus() {
        backend.add(this)
    }

    operator fun Number?.unaryPlus() {
        backend.add(this)
    }

    operator fun Char?.unaryPlus() {
        backend.add(this)
    }

    fun build() = backend
}

inline fun jsonArray(
    builderAction: JsonArrayBuilder.() -> Unit
) = JsonArrayBuilder(JsonArray()).apply(builderAction).build()

@JsonDsl
@JvmInline
value class JsonObjectBuilder(private val backend: JsonObject) {

    operator fun String.invoke(value: JsonElement?) {
        backend.add(this, value)
    }

    operator fun String.invoke(value: Char?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: Number?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: String?) {
        backend.addProperty(this, value)
    }

    operator fun String.invoke(value: Boolean?) {
        backend.addProperty(this, value)
    }

    inline infix fun String.obj(builderAction: JsonObjectBuilder.() -> Unit) {
        invoke(jsonObject(builderAction))
    }

    inline infix fun String.array(builderAction: JsonArrayBuilder.() -> Unit) {
        invoke(jsonArray(builderAction))
    }

    operator fun String.get(vararg elements: JsonElement?) {
        invoke(JsonArray(elements.size).apply { elements.forEach(::add) })
    }

    fun build() = backend
}

inline fun jsonObject(
    builderAction: JsonObjectBuilder.() -> Unit
) = JsonObjectBuilder(JsonObject()).apply(builderAction).build()

inline fun JsonArray.getOrNull(index: Int): JsonElement? =
    if (index in 0 until this.size()) this[index] else null
// codex start
//
// inline fun <T> Iterable<T>.mapToJsonArray(transform: (T) -> JsonElement?): JsonArray {
//     val a = if (this is Collection) JsonArray(this.size) else JsonArray()
//     forEach { a.add(transform(it)) }
//     return a
// }
//
// /**
//  * Unlike [JsonObject.get] followed by `asString` and friends, these return `null` for an absent key
//  * instead of throwing.
//  */
// codex end
fun JsonObject.string(key: String): String? = if (has(key)) get(key).asString else null

// codex start
// fun JsonObject.int(key: String): Int? = if (has(key)) get(key).asInt else null
// codex end

// codex start
// fun JsonObject.long(key: String): Long? = if (has(key)) get(key).asLong else null
// codex end

// codex start
// fun JsonObject.double(key: String): Double? = if (has(key)) get(key).asDouble else null
// codex end

// codex start
// fun JsonObject.boolean(key: String): Boolean? = if (has(key)) get(key).asBoolean else null
// codex end

// codex start
// fun JsonObject.obj(key: String): JsonObject? = if (has(key)) get(key).asJsonObject else null
// codex end

fun JsonObject.array(key: String): JsonArray? = if (has(key)) get(key).asJsonArray else null

// codex start
// fun JsonArray.string(index: Int): String? = getOrNull(index)?.asString
// codex end

// codex start
// fun JsonArray.int(index: Int): Int? = getOrNull(index)?.asInt
// codex end

// codex start
// fun JsonArray.long(index: Int): Long? = getOrNull(index)?.asLong
// codex end

// codex start
// fun JsonArray.double(index: Int): Double? = getOrNull(index)?.asDouble
// codex end

// codex start
// fun JsonArray.boolean(index: Int): Boolean? = getOrNull(index)?.asBoolean
// codex end

// codex start
// fun JsonArray.obj(index: Int): JsonObject? = getOrNull(index)?.asJsonObject
// codex end

// codex start
// fun JsonArray.array(index: Int): JsonArray? = getOrNull(index)?.asJsonArray
// codex end
