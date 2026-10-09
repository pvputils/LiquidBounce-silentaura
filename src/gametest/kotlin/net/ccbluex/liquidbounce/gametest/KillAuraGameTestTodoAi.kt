@file:Suppress("MaxLineLength")

package net.ccbluex.liquidbounce.gametest

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.integration.screen.KillAuraConfigScreenTodoAi
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.combat.Targets
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.utils.client.mc
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.entity.monster.zombie.Zombie

class KillAuraGameTestTodoAi : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitFor({ it.gui.screen() is TitleScreen && LiquidBounce.isInitialized }, 1200)
        context.runOnClient<RuntimeException> {
            check(ModuleManager.any { it === ModuleKillAura })
            check(ModuleManager.none { it.name in setOf("ClickGUI", "HUD", "Reach", "Hitbox") })
            val parent = mc.gui.screen()
            val screen = KillAuraConfigScreenTodoAi(parent)
            mc.gui.setScreen(screen)
            check(mc.gui.screen() === screen)
            check(screen.children().filterIsInstance<Button>().any { it.message.string.startsWith("Enabled:") })
        }
        context.takeScreenshot("KillAuraSettingsTodoAi")
        context.runOnClient<RuntimeException> { mc.gui.screen()!!.onClose() }
        context.worldBuilder().create().use { world ->
            world.server.runCommand("gamemode survival @a")
            world.server.runCommand("time set midnight")
            context.waitFor({ it.player?.onGround() == true }, 200)
            context.runOnClient<RuntimeException> {
                EventManager.callEvent(KeyboardKeyEvent(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RSHIFT),
                    InputConstants.KEY_RSHIFT, 0, InputConstants.PRESS, 0))
                check(mc.gui.screen() is KillAuraConfigScreenTodoAi) { "Right Shift did not open settings" }
                mc.gui.screen()!!.onClose()
            }
            world.server.runCommand("give @a minecraft:iron_sword")
            world.server.runCommand("execute at @p run summon minecraft:zombie ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}")
            context.waitFor({ it.level?.entitiesForRendering()?.any { entity -> entity is Zombie } == true }, 200)
            context.runOnClient<RuntimeException> {
                ModuleKillAura.enabled = true
                ConfigSystem.store(ModuleManager.modulesConfig)
                ModuleKillAura.enabled = false
                ConfigSystem.load(ModuleManager.modulesConfig)
                check(ModuleKillAura.enabled) { "Enabled state was not restored from disk" }
            }
            context.runOnClient<RuntimeException> {
                val zombie = mc.level!!.entitiesForRendering().filterIsInstance<Zombie>().first()
                check(!zombie.shouldBeAttacked(java.util.EnumSet.allOf(Targets::class.java))) {
                    "Mob remains eligible with every target option enabled"
                }
                check(GlobalSettingsTarget.combatChoices.choices.none {
                    it.tag in setOf("Hostile", "Angerable", "WaterCreature", "Passive", "ArmorStand")
                }) { "Non-player target categories are still selectable" }
            }
            context.waitTicks(40)
            context.runOnClient<RuntimeException> {
                val zombie = mc.level!!.entitiesForRendering().filterIsInstance<Zombie>().first()
                check(zombie.health == 20f && zombie.hurtTime == 0) { "KillAura attacked a mob" }
                check(ModuleKillAura.targetTracker.target == null) { "KillAura selected a mob" }
                ModuleKillAura.enabled = false
                ConfigSystem.store(ModuleManager.modulesConfig)
            }
        }
    }
}
