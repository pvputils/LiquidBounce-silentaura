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

import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import org.joml.Vector2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KillAuraValueInputTestTodoAi {
    companion object {
        init { MinecraftBootstrap.ensureInitialized() }
    }

    @Test
    fun `invalid ranges preserve the current setting`() {
        val setting = RangedValue("Speed", defaultValue = 10f..20f, range = 0f..180f,
            suffix = "", valueType = ValueType.FLOAT_RANGE)
        for (input in listOf("20..10", "-1..20", "10..181", "NaN..20", "10..Infinity", "1..2..3")) {
            assertFailsWith<IllegalArgumentException>(input) { KillAuraValueInputTodoAi.apply(setting, input) }
            assertEquals(10f..20f, setting.get())
        }
        KillAuraValueInputTodoAi.apply(setting, "0..180")
        assertEquals(0f..180f, setting.get())
    }

    @Test
    fun `integer settings reject fractions without truncation`() {
        val setting = RangedValue("Delay", defaultValue = 5, range = 0..20,
            suffix = "ticks", valueType = ValueType.INT)
        assertFailsWith<IllegalArgumentException> { KillAuraValueInputTodoAi.apply(setting, "5.5") }
        assertEquals(5, setting.get())
        KillAuraValueInputTodoAi.apply(setting, "20")
        assertEquals(20, setting.get())
    }

    @Test
    fun `curves need two bounded points before replacing the original`() {
        val initial = mutableListOf(Vector2f(0f, 0f), Vector2f(1f, 1f))
        val setting = CurveValue("Curve", initial, CurveValue.Axis("Time", 0f..1f),
            CurveValue.Axis("Factor", 0f..1f))
        for (input in listOf("0,0", "0,0; 2,1", "0,0; 1,NaN", "0,0; 1")) {
            assertFailsWith<IllegalArgumentException>(input) { KillAuraValueInputTodoAi.apply(setting, input) }
            assertEquals(initial, setting.get())
        }
        KillAuraValueInputTodoAi.apply(setting, "0,0; 0.5,0.25; 1,1")
        assertEquals(listOf(Vector2f(0f, 0f), Vector2f(0.5f, 0.25f), Vector2f(1f, 1f)), setting.get())
    }
}
