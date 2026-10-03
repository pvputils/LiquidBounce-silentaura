package net.ccbluex.liquidbounce.gui

import com.mojang.blaze3d.platform.InputConstants
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.list.ListValue
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Vanilla widgets, backed by the complete live KillAura value tree. */
class KillAuraConfigScreenTodoAi(
    private val parent: Screen? = null,
    private val group: ValueGroup = ModuleKillAura,
) : Screen(Component.literal(if (group === ModuleKillAura) "KillAura" else group.name)) {
    private var page = 0
    private var query = ""
    private var error = ""
    private val rowsPerPage get() = ((height - 112) / 24).coerceAtLeast(1)
    private val panelWidth get() = (width - 32).coerceIn(160, 440)
    private val left get() = (width - panelWidth) / 2

    override fun init() {
        clearWidgets()
        val search = EditBox(font, left, 28, panelWidth, 20, Component.literal("Search settings"))
        search.value = query
        search.setHint(Component.literal("Search this section"))
        search.setResponder { query = it; page = 0; rebuildWidgets() }
        addRenderableWidget(search)
        setInitialFocus(search)
        search.moveCursorToEnd(false)

        val values = group.containedValues.filter {
            (!it.notAnOption || it === (group as? ToggleableValueGroup)?.enabledValue) && it.visibleCondition.asBoolean
        }
            .toMutableList()
        if (group === ModuleKillAura) values.add(GlobalSettingsTarget)
        val rows = mutableListOf<Pair<String, () -> Unit>>()
        if (group is ModeValueGroup<*>) {
            for (mode in group.modes) {
                rows += "${if (mode === group.activeMode) "[Selected] " else ""}${mode.name}" to {
                    group.setByString(mode.name)
                    mc.gui.setScreen(KillAuraConfigScreenTodoAi(this, mode))
                }
            }
        }
        for (value in values) {
            rows += label(value) to { open(value) }
        }
        val filtered = rows.filter { it.first.contains(query, ignoreCase = true) }
        val maxPage = ((filtered.size - 1).coerceAtLeast(0)) / rowsPerPage
        page = page.coerceIn(0, maxPage)
        filtered.drop(page * rowsPerPage).take(rowsPerPage).forEachIndexed { index, (label, action) ->
            button(label, left, 56 + index * 24, panelWidth) { action() }
        }
        button("<", left, height - 48, 40) { page--; rebuildWidgets() }.active = page > 0
        button("${page + 1} / ${maxPage + 1}", left + 44, height - 48, 84) { }.active = false
        button(">", left + 132, height - 48, 40) { page++; rebuildWidgets() }.active = page < maxPage
        button(if (parent is KillAuraConfigScreenTodoAi) "Back" else "Done", left + panelWidth - 100,
            height - 48, 100) { onClose() }
    }

    private fun label(value: Value<*>): String = when (value) {
        is ModeValueGroup<*> -> "${value.name}: ${value.activeMode.name} >"
        is ValueGroup -> "${value.name} >"
        else -> "${value.name}: ${when (val v = value.get()) {
            is Boolean -> if (v) "ON" else "OFF"
            is InputBind -> if (v.isUnbound) "None" else v.boundKey.displayName.string
            is Tagged -> v.tag
            is Set<*> -> v.joinToString { (it as? Tagged)?.tag ?: it.toString() }
            else -> SettingEditorTodoAi.text(value).take(64)
        }}"
    }

    @Suppress("UNCHECKED_CAST")
    private fun open(value: Value<*>) {
        if (value is ListValue<*, *> && value.innerValueType == ValueType.TEXT) {
            mc.gui.setScreen(TextListScreenTodoAi(this, value))
            return
        }
        when (value) {
            is ValueGroup -> mc.gui.setScreen(KillAuraConfigScreenTodoAi(this, value))
            is ChoiceListValue<*> -> mc.gui.setScreen(ChoiceScreenTodoAi(this, value))
            is MultiChoiceListValue<*> -> mc.gui.setScreen(ChoiceScreenTodoAi(this, value))
            else -> if (value.get() is Boolean && !value.isImmutable) {
                (value as Value<Boolean>).set(!value.get())
                rebuildWidgets()
            } else {
                mc.gui.setScreen(ValueScreenTodoAi(this, value))
            }
        }
    }

    private fun button(text: String, x: Int, y: Int, w: Int, action: () -> Unit): Button =
        addRenderableWidget(Button.builder(Component.literal(text)) { action() }.bounds(x, y, w, 20).build())

    override fun onClose() {
        runCatching { ConfigSystem.storeAll() }.onFailure { error = "Could not save settings: ${it.message}" }
            .onSuccess { mc.gui.setScreen(parent) }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
        if (error.isNotEmpty()) {
            context.centeredText(font, Component.literal(error), width / 2, height - 22, 0xFFFF5555.toInt())
        }
    }

    companion object : EventListener {
        override val running = true
        @Suppress("unused")
        private val keyHandler = handler<KeyboardKeyEvent> {
            if (it.isPressed && it.scanCode == InputConstants.KEY_RSHIFT && mc.gui.screen() == null) {
                mc.execute { mc.gui.setScreen(KillAuraConfigScreenTodoAi()) }
            }
        }
        fun register() = Unit
    }
}

class KillAuraModMenuTodoAi : ModMenuApi {
    override fun getModConfigScreenFactory() = ConfigScreenFactory { parent -> KillAuraConfigScreenTodoAi(parent) }
}

private class ChoiceScreenTodoAi(private val parent: Screen, private val value: Value<*>) :
    Screen(Component.literal(value.name)) {
    private var page = 0
    override fun init() {
        clearWidgets()
        val choices = when (value) {
            is ChoiceListValue<*> -> value.choices.toList()
            is MultiChoiceListValue<*> -> value.choices.toList()
            else -> emptyList()
        }
        val count = ((height - 80) / 24).coerceAtLeast(1)
        val lastPage = (choices.size - 1).coerceAtLeast(0) / count
        page = page.coerceIn(0, lastPage)
        choices.drop(page * count).take(count).forEachIndexed { index, choice ->
            val selected = value.get() == choice || (value.get() as? Set<*>)?.contains(choice) == true
            addRenderableWidget(Button.builder(Component.literal("${if (selected) "[x]" else "[ ]"} ${choice.tag}")) {
                if (value is MultiChoiceListValue<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (value as MultiChoiceListValue<Tagged>).toggle(choice)
                } else value.setByString(choice.tag)
                rebuildWidgets()
            }.bounds(width / 2 - 130, 32 + index * 24, 260, 20).build())
        }
        addRenderableWidget(Button.builder(Component.literal("<")) { page--; rebuildWidgets() }
            .bounds(width / 2 - 130, height - 32, 40, 20).build()).active = page > 0
        addRenderableWidget(Button.builder(Component.literal("Back")) { onClose() }
            .bounds(width / 2 - 80, height - 32, 160, 20).build())
        addRenderableWidget(Button.builder(Component.literal(">")) { page++; rebuildWidgets() }
            .bounds(width / 2 + 90, height - 32, 40, 20).build()).active = page < lastPage
    }
    override fun onClose() = mc.gui.setScreen(parent)
    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
    }
}

private class ValueScreenTodoAi(private val parent: Screen, private val value: Value<*>) :
    Screen(Component.literal(value.name)) {
    private lateinit var input: EditBox
    private var draft = SettingEditorTodoAi.text(value)
    private var bindAction = (value.get() as? InputBind)?.action
    private val modifiers = (value.get() as? InputBind)?.modifiers?.toMutableSet() ?: mutableSetOf()
    private var error = ""

    override fun init() {
        val w = (width - 40).coerceAtMost(440)
        val x = (width - w) / 2
        input = EditBox(font, x, 58, w, 20, title)
        input.setMaxLength(16384)
        input.value = draft
        input.setResponder { draft = it }
        input.setEditable(!value.isImmutable)
        value.description.get()?.let { input.setTooltip(Tooltip.create(Component.literal(it))) }
        addRenderableWidget(input)
        setInitialFocus(input)
        if (bindAction != null) {
            addRenderableWidget(Button.builder(Component.literal("Action: $bindAction")) {
                val actions = InputBind.BindAction.entries
                bindAction = actions[(actions.indexOf(bindAction) + 1) % actions.size]
                rebuildWidgets()
            }.bounds(x, 84, w, 20).build())
            InputBind.Modifier.entries.forEachIndexed { index, modifier ->
                val label = "${if (modifier in modifiers) "[x]" else "[ ]"} ${modifier.tag}"
                addRenderableWidget(Button.builder(Component.literal(label)) {
                    if (!modifiers.remove(modifier)) modifiers.add(modifier)
                    rebuildWidgets()
                }.bounds(x + index * (w / InputBind.Modifier.entries.size), 108,
                    w / InputBind.Modifier.entries.size - 2, 20).build())
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Apply")) { apply() }
            .bounds(x, height - 32, w / 3 - 4, 20).build()).active = !value.isImmutable
        addRenderableWidget(Button.builder(Component.literal("Reset")) {
            value.restore()
            mc.gui.setScreen(parent)
        }.bounds(x + w / 3, height - 32, w / 3 - 4, 20).build()).active = !value.isImmutable
        addRenderableWidget(Button.builder(Component.literal("Cancel")) { onClose() }
            .bounds(x + 2 * w / 3, height - 32, w / 3, 20).build())
    }

    private fun apply() {
        runCatching {
            SettingEditorTodoAi.apply(value, input.value)
            if (bindAction != null) {
                @Suppress("UNCHECKED_CAST")
                val binding = value as Value<InputBind>
                binding.set(binding.get().copy(action = requireNotNull(bindAction), modifiers = modifiers.toSet()))
            }
        }.onSuccess { mc.gui.setScreen(parent) }.onFailure { error = it.message ?: "Invalid value" }
    }
    override fun onClose() = mc.gui.setScreen(parent)
    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
        val w = (width - 40).coerceAtMost(440)
        font.split(Component.literal(SettingEditorTodoAi.hint(value)), w).take(3).forEachIndexed { index, line ->
            context.text(font, line, (width - w) / 2, 30 + index * 9, -1, false)
        }
        if (error.isNotEmpty()) {
            font.split(Component.literal(error), w).take(3).forEachIndexed { index, line ->
                context.text(font, line, (width - w) / 2, height - 68 + index * 9, 0xFFFF5555.toInt(), false)
            }
        }
    }
}
