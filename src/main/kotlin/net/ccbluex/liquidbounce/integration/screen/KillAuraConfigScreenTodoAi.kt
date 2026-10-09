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
@file:Suppress("TooManyFunctions", "LargeClass", "MaxLineLength")

package net.ccbluex.liquidbounce.integration.screen

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.fileGson
import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import org.joml.Vector2f

private const val ROW_HEIGHT = 24
private const val CONTROL_WIDTH = 300

/** Native settings pages share the live config values, including every mode's children. */
class KillAuraConfigScreenTodoAi(
    private val parentScreen: Screen?,
    private val group: ValueGroup = ModuleKillAura,
    private var page: Int = 0,
) : Screen(Component.literal("KillAura / ${group.name}")) {
    private val rowsPerPage get() = ((height - 104) / ROW_HEIGHT).coerceAtLeast(1)
    private val controlsWidth get() = CONTROL_WIDTH.coerceAtMost(width - 24)
    private val left get() = (width - controlsWidth) / 2

    private fun entries(): List<Value<*>> = buildList {
        if (group is ModeValueGroup<*>) addAll(group.modes) else addAll(group.get())
        if (group === ModuleKillAura) {
            add(GlobalSettingsTarget)
            addAll(ModuleManager.filter { it !== ModuleKillAura })
        }
    }.filterNot { it.notAnOption && it.name != "Enabled" }

    override fun init() {
        if (!LiquidBounce.isInitialized) {
            button("Client is still loading", 44) {}.active = false
            button("Done", height - 28) { onClose() }
            return
        }
        val values = entries()
        val pageCount = ((values.size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)
        page = page.coerceIn(0, pageCount - 1)
        if (group is ModeValueGroup<*>) {
            button("Selected: ${group.activeMode.name}", 28) {
                val next = (group.modes.indexOf(group.activeMode) + 1) % group.modes.size
                group.setByString(group.modes[next].name)
                rebuildWidgets()
            }
        }
        values.drop(page * rowsPerPage).take(rowsPerPage).forEachIndexed { index, value ->
            val label = when (value) {
                is ValueGroup -> "${value.name}..."
                is MultiChoiceListValue<*> -> "${value.name}: ${value.get().joinToString { it.tag }}"
                else -> "${value.name}: ${displayValue(value)}"
            }
            val control = button(label, 54 + index * ROW_HEIGHT) { edit(value) }
            control.active = !value.isImmutable
            value.description.get()?.let { control.setTooltip(Tooltip.create(Component.literal(it))) }
        }
        addRenderableWidget(Button.builder(Component.literal("<")) {
            page--; rebuildWidgets()
        }.bounds(left, height - 52, 40, 20).build()).active = page > 0
        addRenderableWidget(Button.builder(Component.literal(">")) {
            page++; rebuildWidgets()
        }.bounds(left + controlsWidth - 40, height - 52, 40, 20).build()).active = page + 1 < pageCount
        button("Done", height - 28) { onClose() }
    }

    private fun button(label: String, y: Int, action: () -> Unit): Button = addRenderableWidget(
        Button.builder(Component.literal(label)) { action() }.bounds(left, y, controlsWidth, 20).build()
    )

    private fun edit(value: Value<*>) {
        when (value) {
            is ValueGroup -> mc.gui.setScreen(KillAuraConfigScreenTodoAi(this, value))
            is MultiChoiceListValue<*> -> mc.gui.setScreen(KillAuraChoicesScreenTodoAi(this, value))
            is ChoiceListValue<*> -> {
                val choices = value.choices.toList()
                value.setByString(choices[(choices.indexOf(value.get()) + 1) % choices.size].tag)
                rebuildWidgets()
            }
            // codex start
            // is BindValue -> mc.gui.setScreen(KillAuraBindScreenTodoAi(this, value))
            // codex end
            else -> if (value.get() is Boolean) {
                value.setByString((value.get() != true).toString())
                rebuildWidgets()
            } else {
                mc.gui.setScreen(KillAuraValueScreenTodoAi(this, value))
            }
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
        if (LiquidBounce.isInitialized) {
            val count = ((entries().size + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1)
            context.centeredText(font, Component.literal("${page + 1} / $count"), width / 2, height - 46, -1)
        }
    }

    override fun onClose() {
        if (LiquidBounce.isInitialized) persistKillAuraSettingsTodoAi()
        mc.gui.setScreen(parentScreen)
    }
}

private fun displayValue(value: Value<*>): String = when (val current = value.get()) {
    is Boolean -> if (current) "On" else "Off"
    is Tagged -> current.tag
    is ClosedRange<*> -> "${current.start}..${current.endInclusive}"
    is Color4b -> "#%08X".format(current.argb)
    // codex start
    // is InputBind -> "${current.boundKey.displayName.string} (${current.action.tag})"
    // codex end
    is java.io.File -> current.path
    is String, is Number -> current.toString()
    else -> fileGson.toJson(current)
}

private class KillAuraChoicesScreenTodoAi(
    private val parentScreen: Screen,
    private val value: MultiChoiceListValue<*>,
    private var page: Int = 0,
) : Screen(Component.literal(value.name)) {
    override fun init() {
        val count = ((height - 80) / ROW_HEIGHT).coerceAtLeast(1)
        val choices = (value.get() + value.choices).distinct()
        val pages = ((choices.size + count - 1) / count).coerceAtLeast(1)
        page = page.coerceIn(0, pages - 1)
        choices.drop(page * count).take(count).forEachIndexed { index, choice ->
            val active = choice in value.get()
            addRenderableWidget(Button.builder(Component.literal("${choice.tag}: ${if (active) "On" else "Off"}")) {
                toggle(choice)
                rebuildWidgets()
            }.bounds(width / 2 - 140, 36 + index * ROW_HEIGHT, 240, 20).build())
            if (value.isOrderSensitive && active) {
                addRenderableWidget(Button.builder(Component.literal("↑")) {
                    val selected = value.get().toMutableList()
                    val position = selected.indexOf(choice)
                    if (position > 0) java.util.Collections.swap(selected, position, position - 1)
                    val json = JsonArray().apply { selected.forEach { add(it.tag) } }
                    value.deserializeFrom(fileGson, json)
                    rebuildWidgets()
                }.bounds(width / 2 + 104, 36 + index * ROW_HEIGHT, 36, 20).build())
            }
        }
        addRenderableWidget(Button.builder(Component.literal("<")) {
            page--; rebuildWidgets()
        }.bounds(width / 2 - 140, height - 28, 40, 20).build()).active = page > 0
        addRenderableWidget(Button.builder(Component.literal(">")) {
            page++; rebuildWidgets()
        }.bounds(width / 2 + 100, height - 28, 40, 20).build()).active = page + 1 < pages
        addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
            .bounds(width / 2 - 90, height - 28, 180, 20).build())
    }

    @Suppress("UNCHECKED_CAST")
    private fun toggle(choice: Tagged) = (value as MultiChoiceListValue<Tagged>).toggle(choice)

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
    }

    override fun onClose() {
        persistKillAuraSettingsTodoAi()
        mc.gui.setScreen(parentScreen)
    }
}

private class KillAuraValueScreenTodoAi(
    private val parentScreen: Screen,
    private val value: Value<*>,
) : Screen(Component.literal(value.name)) {
    private lateinit var field: EditBox
    private var draft: String = if (value is CurveValue) {
        value.get().joinToString("; ") { "${it.x},${it.y}" }
    } else {
        displayValue(value)
    }
    private var error = ""

    override fun init() {
        field = addRenderableWidget(EditBox(font, width / 2 - 140, 60, 280, 20, title))
        field.setMaxLength(8192)
        field.value = draft
        field.setResponder { draft = it }
        setInitialFocus(field)
        addRenderableWidget(Button.builder(Component.literal("Apply")) {
            runCatching { KillAuraValueInputTodoAi.apply(value, field.value) }.onSuccess { onClose() }
                .onFailure { error = it.message ?: "Invalid value" }
        }.bounds(width / 2 - 140, 112, 135, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Cancel")) { onClose() }
            .bounds(width / 2 + 5, 112, 135, 20).build())
        addRenderableWidget(Button.builder(Component.literal("Reset to default")) {
            value.restore()
            onClose()
        }.bounds(width / 2 - 140, 140, 280, 20).build())
    }


    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 20, -1)
        val hint = when (value) {
            is RangedValue<*> -> "Range: ${value.range.start}..${value.range.endInclusive} ${value.suffix}"
            is CurveValue -> "Points: x,y; x,y (${value.xAxis.label}, ${value.yAxis.label})"
            else -> "Enter ${value.name}"
        }
        context.centeredText(font, Component.literal(hint), width / 2, 42, -1)
        if (error.isNotEmpty()) context.centeredText(font, Component.literal(error), width / 2, 88, 0xFFFF5555.toInt())
    }

    override fun onClose() {
        persistKillAuraSettingsTodoAi()
        mc.gui.setScreen(parentScreen)
    }
}

// codex start
// private class KillAuraBindScreenTodoAi(
//     private val parentScreen: Screen,
//     private val value: BindValue,
// ) : Screen(Component.literal("KillAura key binding")) {
//     private var capturing = false
//
//     override fun init() {
//         addRenderableWidget(Button.builder(Component.literal("Key: ${value.get().boundKey.displayName.string}")) {
//             capturing = true
//             it.message = Component.literal("Press a key (Esc cancels)")
//         }.bounds(width / 2 - 140, 40, 280, 20).build())
//         addRenderableWidget(Button.builder(Component.literal("Action: ${value.get().action.tag}")) {
//             val actions = InputBind.BindAction.entries
//             value.set(value.get().copy(action = actions[(actions.indexOf(value.get().action) + 1) % actions.size]))
//             rebuildWidgets()
//         }.bounds(width / 2 - 140, 64, 280, 20).build())
//         InputBind.Modifier.entries.forEachIndexed { index, modifier ->
//             val label = "${modifier.tag}: ${modifier in value.get().modifiers}"
//             addRenderableWidget(Button.builder(Component.literal(label)) {
//                 val modifiers = value.get().modifiers.toMutableSet()
//                 if (!modifiers.remove(modifier)) modifiers.add(modifier)
//                 value.set(value.get().copy(modifiers = modifiers))
//                 rebuildWidgets()
//             }.bounds(width / 2 - 140, 88 + index * ROW_HEIGHT, 280, 20).build())
//         }
//         addRenderableWidget(Button.builder(Component.literal("Unbind")) {
//             value.set(InputBind.UNBOUND)
//             rebuildWidgets()
//         }.bounds(width / 2 - 140, height - 52, 280, 20).build())
//         addRenderableWidget(Button.builder(Component.literal("Done")) { onClose() }
//             .bounds(width / 2 - 140, height - 28, 280, 20).build())
//     }
//
//     override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
//         if (!capturing) return super.mouseClicked(event, doubleClick)
//         capturing = false
//         value.set(value.get().copy(boundKey = InputConstants.Type.MOUSE.getOrCreate(event.button())))
//         rebuildWidgets()
//         return true
//     }
//
//     override fun keyPressed(event: KeyEvent): Boolean {
//         if (!capturing) return super.keyPressed(event)
//         capturing = false
//         if (event.key() != InputConstants.KEY_ESCAPE) {
//             value.set(value.get().copy(boundKey = InputConstants.Type.KEYBOARD.getOrCreate(event.key())))
//         }
//         rebuildWidgets()
//         return true
//     }
//
//     override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
//         super.extractRenderState(context, mouseX, mouseY, partialTick)
//         context.centeredText(font, title, width / 2, 12, -1)
//     }
//
//     override fun onClose() {
//         persistKillAuraSettingsTodoAi()
//         mc.gui.setScreen(parentScreen)
//     }
// }
// codex end

private fun persistKillAuraSettingsTodoAi() {
    ConfigSystem.store(ModuleManager.modulesConfig)
    ConfigSystem.store(GlobalManager)
}

internal object KillAuraConfigAccessTodoAi : EventListener {
    @Suppress("unused")
    private val openSettings = handler<KeyboardKeyEvent> { event ->
        if (LiquidBounce.isInitialized && event.isPressed &&
            event.key.value == InputConstants.KEY_RSHIFT && mc.gui.screen() == null) {
            mc.gui.setScreen(KillAuraConfigScreenTodoAi(null))
        }
    }
}

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

/** All screen registration and settings editors are isolated in this file. */
class KillAuraSettingsEntrypointTodoAi : ClientModInitializer {
    override fun onInitializeClient() {
        KillAuraConfigAccessTodoAi
    }
}
