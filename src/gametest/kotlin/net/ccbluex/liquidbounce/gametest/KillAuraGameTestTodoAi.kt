@file:Suppress("MaxLineLength")

package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.integration.screen.KillAuraConfigScreenTodoAi
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
            check(ModuleManager.map { it.name }.toSet() == setOf("KillAura", "AutoWeapon", "AntiBot", "Teams",
                "TargetLock", "Debug", "SwordBlock", "Criticals", "ElytraTarget", "MultiActions"))
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
            context.waitFor({ it.player?.onGround() == true }, 200)
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
            context.waitFor({ client ->
                client.level?.entitiesForRendering()?.filterIsInstance<Zombie>()?.any { it.hurtTime > 0 || it.health < 20f } == true
            }, 200)
            context.runOnClient<RuntimeException> {
                check(ModuleKillAura.targetTracker.target != null) { "No target tracked after the attack" }
                ModuleKillAura.enabled = false
                ConfigSystem.store(ModuleManager.modulesConfig)
            }
        }
    }
}
