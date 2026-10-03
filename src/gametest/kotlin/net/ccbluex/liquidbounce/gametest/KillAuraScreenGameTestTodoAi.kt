package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gui.KillAuraConfigScreenTodoAi
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox

class KillAuraScreenGameTestTodoAi : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        context.onClient {
            check(ModuleManager.map { it.name } == listOf("KillAura"))
            mc.gui.setScreen(KillAuraConfigScreenTodoAi(mc.gui.screen()))
        }
        context.waitTicks(5)
        context.takeScreenshot("killaura-native-rootTodoAi")
        context.onClient {
            val search = mc.gui.screen()!!.children().filterIsInstance<EditBox>().single()
            search.value = "Rotations"
            click("Rotations")
            check(mc.gui.screen()!!.title.string == "Rotations")
            click("AngleSmooth")
            check(mc.gui.screen()!!.children().filterIsInstance<Button>().any { it.message.string.contains("Linear") })
        }
        context.waitTicks(3)
        context.takeScreenshot("killaura-native-rotationsTodoAi")
        context.onClient {
            val rotations = ModuleKillAura.containedValues.filterIsInstance<ModeValueGroup<*>>()
            check(rotations.isEmpty()) // Rotation choices live in the inherited Rotations group.
            val groups = ModuleKillAura.collectValueGroupsRecursively().toList()
            val smoothing = groups.filterIsInstance<ModeValueGroup<*>>().single { it.name == "AngleSmooth" }
            check(smoothing.modes.map { it.name }.toSet() ==
                setOf("Linear", "Sigmoid", "Interpolation", "Acceleration", "AI"))
            val rendering = groups.single { it.name == "TargetRendering" }
            val appearances = rendering.containedValues.filterIsInstance<ModeValueGroup<*>>().single()
            check(appearances.modes.size == 8)
            verifyAllGroupsOpen(ModuleKillAura)
            verifyTextEditor(appearances)
            val keepSprint = ModuleKillAura.containedValues.single { it.name == "KeepSprint" }
            @Suppress("UNCHECKED_CAST")
            val setting = keepSprint as Value<Boolean>
            val initial = setting.get()
            try {
                mc.gui.setScreen(KillAuraConfigScreenTodoAi())
                mc.gui.screen()!!.children().filterIsInstance<EditBox>().single().value = "KeepSprint"
                click("KeepSprint")
                check(setting.get() == !initial)
                mc.gui.screen()!!.onClose()
                setting.set(initial)
                ConfigSystem.load(ModuleManager.modulesConfig)
                check(setting.get() == !initial) { "Settings were not persisted" }
            } finally {
                setting.set(initial)
                ConfigSystem.store(ModuleManager.modulesConfig)
            }
            mc.gui.setScreen(net.minecraft.client.gui.screens.TitleScreen())
        }
        verifyInGame(context)
    }

    private fun verifyTextEditor(appearances: ModeValueGroup<*>) {
        val textMode = appearances.modes.single { it.name == "Text2D" }
        val textValue = textMode.containedValues.single { it.name == "Text" }
        val savedText = (textValue.get() as Collection<*>).map { it as String }.toMutableList()
        mc.gui.setScreen(KillAuraConfigScreenTodoAi(null, textMode))
        mc.gui.screen()!!.children().filterIsInstance<EditBox>().single().value = "Text:"
        click("Text:")
        mc.gui.screen()!!.children().filterIsInstance<EditBox>().first().value = "Target, [name]"
        click("Apply")
        check((textValue.get() as Collection<*>).first() == "Target, [name]")
        @Suppress("UNCHECKED_CAST")
        (textValue as Value<MutableCollection<String>>).set(savedText)
    }

    private fun verifyInGame(context: ClientGameTestContext) {
        context.worldBuilder().create().use {
            context.waitFor { client -> client.player != null && client.gui.screen() == null }
            val frames = java.util.concurrent.atomic.AtomicInteger()
            val listener = object : net.ccbluex.liquidbounce.event.EventListener { }
            val hook = context.fromClient {
                listener.on(net.ccbluex.liquidbounce.event.events.OverlayRenderEvent::class.java) {
                    frames.incrementAndGet()
                }
            }
            try {
                context.waitTicks(10)
                check(frames.get() > 0) { "KillAura's 2D target-rendering event was removed" }
                context.input.pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT)
                context.waitForScreen(KillAuraConfigScreenTodoAi::class.java)
                context.takeScreenshot("killaura-native-ingameTodoAi")
                context.onClient { mc.gui.screen()!!.onClose() }
                context.waitForScreen(null)
            } finally {
                context.onClient { hook.close() }
            }
        }
    }

    private fun verifyAllGroupsOpen(group: ValueGroup) {
        mc.gui.setScreen(KillAuraConfigScreenTodoAi(null, group))
        check(mc.gui.screen() is KillAuraConfigScreenTodoAi)
        if (group is ToggleableValueGroup) {
            check(mc.gui.screen()!!.children().filterIsInstance<Button>().any {
                it.message.string.startsWith("Enabled:")
            }) { "Missing toggle for ${group.name}" }
        }
        if (group is ModeValueGroup<*>) group.modes.forEach(::verifyAllGroupsOpen)
        group.containedValues.filterIsInstance<ValueGroup>().forEach(::verifyAllGroupsOpen)
    }

    private fun click(prefix: String) {
        val button = mc.gui.screen()!!.children().filterIsInstance<Button>()
            .first { it.message.string.startsWith(prefix) }
        button.onPress(net.minecraft.client.input.KeyEvent(0, 0, 0))
    }
}
