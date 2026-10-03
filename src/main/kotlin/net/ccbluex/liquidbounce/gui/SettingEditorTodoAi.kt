package net.ccbluex.liquidbounce.gui

import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.input.inputByName
import com.mojang.blaze3d.platform.InputConstants
import org.joml.Vector2f
import org.joml.Vector2fc
import net.minecraft.world.phys.Vec3
import java.io.File

/** Text controls use the original value setters, including their change listeners. */
internal object SettingEditorTodoAi {
    fun text(value: Value<*>): String = when (val current = value.get()) {
        is Color4b -> "#${current.toHexString()}"
        is File -> current.path
        is InputBind -> current.boundKey.name
        is Vec3 -> "${current.x}, ${current.y}, ${current.z}"
        is Vector2fc -> "${current.x()}, ${current.y()}"
        else -> if (value is CurveValue) {
            value.get().joinToString("; ") { "${it.x}, ${it.y}" }
        } else {
            current.toString()
        }
    }

    fun hint(value: Value<*>): String = when (value) {
        is RangedValue<*> -> "Allowed: ${value.range} ${value.suffix}" +
            if (value.get() is ClosedRange<*>) " — enter minimum..maximum" else ""
        is CurveValue -> "Points: x,y; x,y — ${value.xAxis.label}: ${value.xAxis.range}; " +
            "${value.yAxis.label}: ${value.yAxis.range}"
        else -> when (value.get()) {
            is Color4b -> "Color: #RRGGBB or #AARRGGBB (alpha first)"
            is InputBind -> "Key name, e.g. R, mouse.4 or NONE; action and modifiers below"
            is File -> "Path to the texture file"
            is Vec3 -> "Coordinates: x, y, z"
            is Vector2fc -> "Coordinates: x, y"
            else -> "Enter a value and choose Apply"
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(value: Value<*>, input: String) {
        require(!value.isImmutable) { "This setting is read-only" }
        if (value is RangedValue<*>) {
            val parts = input.split("..").map { it.trim().toDouble() }
            val expected = if (value.get() is ClosedRange<*>) 2 else 1
            require(parts.size == expected) { "Enter ${if (expected == 2) "minimum..maximum" else "one number"}" }
            val low = (value.range.start as Number).toDouble()
            val high = (value.range.endInclusive as Number).toDouble()
            require(parts.all { it.isFinite() && it in low..high }) { "Value must be between $low and $high" }
            require(parts.first() <= parts.last()) { "Minimum must not exceed maximum" }
        }
        if (value.get() is InputBind) {
            require(input.equals("NONE", true) || input == InputConstants.UNKNOWN.name ||
                inputByName(input) != InputConstants.UNKNOWN) { "Unknown key name" }
        }
        when {
            value is CurveValue -> {
                val points = input.split(';').map { pair ->
                    val xy = pair.split(',').map { it.trim().toFloat() }
                    require(xy.size == 2) { "Use x,y; x,y for curve points" }
                    require(xy[0].isFinite() && xy[1].isFinite() &&
                        xy[0] in value.xAxis.range && xy[1] in value.yAxis.range) { "Curve point outside axis bounds" }
                    Vector2f(xy[0], xy[1])
                }
                require(points.size >= 2) { "A curve needs at least two points" }
                require(points.zipWithNext().all { (a, b) -> a.x < b.x }) { "Curve X values must increase" }
                value.set(points.toMutableList())
            }
            value.get() is Vec3 -> {
                val xyz = input.split(',').map { it.trim().toDouble() }
                require(xyz.size == 3 && xyz.all { it.isFinite() }) { "Enter three finite coordinates" }
                (value as Value<Vec3>).set(Vec3(xyz[0], xyz[1], xyz[2]))
            }
            value.get() is Vector2fc -> {
                val xy = input.split(',').map { it.trim().toFloat() }
                require(xy.size == 2 && xy.all { it.isFinite() }) { "Enter two finite coordinates" }
                (value as Value<Vector2fc>).set(Vector2f(xy[0], xy[1]))
            }
            else -> value.setByString(input)
        }
    }
}
