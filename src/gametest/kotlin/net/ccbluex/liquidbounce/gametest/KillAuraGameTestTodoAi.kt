@file:Suppress("MaxLineLength", "LongMethod")

package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import java.util.concurrent.atomic.AtomicInteger
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
            world.server.runCommand("time set midnight")
            context.waitFor({ it.player?.onGround() == true }, 200)
            world.server.runCommand("give @a minecraft:iron_sword")
            world.server.runCommand("execute at @p run summon minecraft:zombie ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}")
            context.waitFor({ it.level?.entitiesForRendering()?.any { entity -> entity is Zombie } == true }, 200)
            val attacks = AtomicInteger()
            val listener = object : EventListener {}
            context.runOnClient<RuntimeException> {
                EventManager.registerEventHook(AttackEntityEvent::class.java, EventHook<AttackEntityEvent>(listener) {
                    if (it.entity is Zombie) attacks.incrementAndGet()
                })
                check(ModuleKillAura.get().none { it.name == "Clicker" })
                ModuleKillAura.enabled = true
                ConfigSystem.store(ModuleManager.modulesConfig)
                ModuleKillAura.enabled = false
                ConfigSystem.load(ModuleManager.modulesConfig)
                check(ModuleKillAura.enabled) { "Enabled state was not restored from disk" }
            }
            context.waitFor({ ModuleKillAura.targetTracker.target != null }, 200)
            context.waitTicks(40)
            check(attacks.get() == 0) { "KillAura attacked without a real input press" }
            context.input.holdKey { it.keyAttack }
            context.waitTicks(40)
            check(attacks.get() == 1) { "Holding attack generated ${attacks.get()} hits instead of one" }
            context.input.releaseKey { it.keyAttack }
            context.waitTicks(20)
            check(attacks.get() == 1) { "KillAura repeated a hit after release" }
            context.waitFor({ client ->
                client.level?.entitiesForRendering()?.filterIsInstance<Zombie>()?.any { it.hurtTime > 0 || it.health < 20f } == true
            }, 200)
            world.server.runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2")
            context.waitTicks(30)
            context.input.pressKey { it.keyAttack }
            context.waitTicks(5)
            check(attacks.get() == 2) { "A second real click did not produce exactly one additional hit" }
            world.server.runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2")
            context.waitTicks(20)
            context.input.pressKey { it.keyAttack }
            context.input.pressKey { it.keyAttack }
            context.waitTicks(5)
            check(attacks.get() == 4) { "Two fast physical presses were dropped or generated extra attacks" }
            context.runOnClient<RuntimeException> {
                EventManager.unregisterEventHandler(listener)
                ModuleKillAura.enabled = false
                ConfigSystem.store(ModuleManager.modulesConfig)
            }
        }
    }
}
