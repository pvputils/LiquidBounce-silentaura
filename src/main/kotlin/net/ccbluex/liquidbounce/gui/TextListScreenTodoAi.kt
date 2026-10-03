package net.ccbluex.liquidbounce.gui

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Editable text lines for Text2D target rendering, without lossy delimiter parsing. */
internal class TextListScreenTodoAi(private val parent: Screen, private val value: Value<*>) :
    Screen(Component.literal(value.name)) {
    private val lines = (value.get() as Collection<*>).map { it as String }.toMutableList()
    private var page = 0
    private val capacity get() = ((height - 104) / 24).coerceAtLeast(1)

    override fun init() {
        val panelWidth = (width - 32).coerceAtMost(440)
        val x = (width - panelWidth) / 2
        val last = (lines.size - 1).coerceAtLeast(0) / capacity
        page = page.coerceIn(0, last)
        lines.drop(page * capacity).take(capacity).forEachIndexed { row, text ->
            val index = page * capacity + row
            val y = 32 + row * 24
            val field = EditBox(font, x, y, panelWidth - 80, 20, Component.literal("Line ${index + 1}"))
            field.setMaxLength(16384)
            field.value = text
            field.setResponder { lines[index] = it }
            addRenderableWidget(field)
            button("Up", x + panelWidth - 76, y, 36) {
                java.util.Collections.swap(lines, index, index - 1)
                rebuildWidgets()
            }.active = index > 0
            button("X", x + panelWidth - 36, y, 36) { lines.removeAt(index); rebuildWidgets() }
        }
        button("<", x, height - 60, 40) { page--; rebuildWidgets() }.active = page > 0
        button("Add line", x + 44, height - 60, panelWidth - 88) {
            lines.add("")
            page = (lines.size - 1) / capacity
            rebuildWidgets()
        }
        button(">", x + panelWidth - 40, height - 60, 40) { page++; rebuildWidgets() }.active = page < last
        button("Apply", x, height - 32, panelWidth / 3 - 4) {
            @Suppress("UNCHECKED_CAST")
            (value as Value<MutableCollection<String>>).set(lines.toMutableList())
            mc.gui.setScreen(parent)
        }
        button("Reset", x + panelWidth / 3, height - 32, panelWidth / 3 - 4) {
            value.restore()
            mc.gui.setScreen(parent)
        }
        button("Cancel", x + 2 * panelWidth / 3, height - 32, panelWidth / 3) { onClose() }
    }

    private fun button(text: String, x: Int, y: Int, w: Int, action: () -> Unit) =
        addRenderableWidget(Button.builder(Component.literal(text)) { action() }.bounds(x, y, w, 20).build())

    override fun onClose() = mc.gui.setScreen(parent)

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(context, mouseX, mouseY, partialTick)
        context.centeredText(font, title, width / 2, 12, -1)
    }
}
