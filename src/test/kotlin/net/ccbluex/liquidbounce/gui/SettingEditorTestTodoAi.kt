package net.ccbluex.liquidbounce.gui

import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.test.MinecraftBootstrap
import org.joml.Vector2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SettingEditorTestTodoAi {
    init { MinecraftBootstrap.ensureInitialized() }

    @Test
    fun `range edits reject inverted nonfinite and out of bounds input without mutation`() {
        val range = RangedValue("Range", defaultValue = 2f..4f, range = 0f..8f,
            suffix = "blocks", valueType = ValueType.FLOAT_RANGE)
        for (invalid in listOf("5..2", "NaN..4", "2..Infinity", "-1..3", "2..9", "3")) {
            assertFailsWith<IllegalArgumentException> { SettingEditorTodoAi.apply(range, invalid) }
            assertEquals(2f..4f, range.get())
        }
        SettingEditorTodoAi.apply(range, "3..6")
        assertEquals(3f..6f, range.get())
    }

    @Test
    fun `curve edits round trip and reject invalid points without mutation`() {
        val curve = CurveValue("Curve", mutableListOf(Vector2f(0f, 0f), Vector2f(1f, 1f)),
            CurveValue.Axis("Time", 0f..1f), CurveValue.Axis("Value", 0f..1f))
        val initial = SettingEditorTodoAi.text(curve)
        for (invalid in listOf("0,0", "0,0; 0,1", "0,0; 2,1", "0,0; 1,NaN")) {
            assertFailsWith<IllegalArgumentException> { SettingEditorTodoAi.apply(curve, invalid) }
            assertEquals(initial, SettingEditorTodoAi.text(curve))
        }
        SettingEditorTodoAi.apply(curve, "0,0; 0.5,0.75; 1,1")
        assertEquals(3, curve.get().size)
        assertEquals(Vector2f(0.5f, 0.75f), curve.get()[1])
    }
}
