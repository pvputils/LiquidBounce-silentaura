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
package net.ccbluex.liquidbounce.integration.screen

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import org.joml.Vector2f

/** Reject invalid inputs before modifying the live configuration. */
internal object KillAuraValueInputTodoAi {
    fun apply(value: Value<*>, text: String) {
        if (value is RangedValue<*>) {
            val bounds = value.range
            val lower = (bounds.start as Number).toDouble()
            val upper = (bounds.endInclusive as Number).toDouble()
            val numbers = text.split("..").map { it.toDouble() }
            require(numbers.all { it.isFinite() && it in lower..upper }) { "Enter a value from $lower to $upper" }
            require(numbers.size <= 2 && (numbers.size == 1 || numbers[0] <= numbers[1])) {
                "The minimum must not exceed the maximum"
            }
        }
        if (value is CurveValue) {
            val points = text.split(';').map {
                val pair = it.trim().split(',')
                require(pair.size == 2) { "Enter points as x,y; x,y" }
                Vector2f(pair[0].trim().toFloat(), pair[1].trim().toFloat())
            }
            require(points.size >= 2 && points.all { it.x in value.xAxis.range && it.y in value.yAxis.range }) {
                "Enter at least two points within the axis ranges"
            }
            value.set(points.toMutableList())
        } else if (value.valueType.deserializer != null || value is RangedValue<*>) {
            value.setByString(text)
        } else {
            value.deserializeFrom(fileGson, JsonParser.parseString(text))
        }
    }

}
