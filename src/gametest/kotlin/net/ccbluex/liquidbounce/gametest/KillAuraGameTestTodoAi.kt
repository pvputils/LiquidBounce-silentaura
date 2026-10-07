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
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.LivingEntity
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.utils.combat.Targets
import net.ccbluex.liquidbounce.utils.client.interaction
import kotlin.math.abs

class KillAuraGameTestTodoAi : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitFor({ it.gui.screen() is TitleScreen && LiquidBounce.isInitialized }, 1200)
        context.runOnClient<RuntimeException> {
            check(ModuleManager.map { it.name }.toSet() == setOf("KillAura", "Debug", "MultiActions"))
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
                check(ModuleKillAura.get().none { it.name in setOf("Clicker", "Criticals", "KeepSprint", "FightBot", "FailSwing") })
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
            world.server.runCommand("kill @e[type=minecraft:zombie]")
            context.runOnClient<RuntimeException> { GlobalSettingsTarget.combat.add(Targets.PASSIVE) }
            try {
                val vanillaGround = measureDamage(context, world.server::runCommand, false, false)
                val auraGround = measureDamage(context, world.server::runCommand, true, false)
                val vanillaFalling = measureDamage(context, world.server::runCommand, false, true)
                val auraFalling = measureDamage(context, world.server::runCommand, true, true)
                check(abs(vanillaGround - auraGround) < 0.01f) { "Grounded damage differs from vanilla" }
                check(abs(vanillaFalling - auraFalling) < 0.01f) { "Critical damage differs from vanilla" }
                check(vanillaFalling > vanillaGround) { "Falling baseline did not produce a vanilla critical: ground=$vanillaGround, falling=$vanillaFalling" }
            } finally {
                context.runOnClient<RuntimeException> {
                    GlobalSettingsTarget.combat.remove(Targets.PASSIVE)
                    ModuleKillAura.enabled = false
                    ConfigSystem.store(ModuleManager.modulesConfig)
                }
            }
        }
    }

    /** Compare identical natural player states against Minecraft's own attack implementation. */
    private fun measureDamage(
        context: ClientGameTestContext,
        command: (String) -> Unit,
        aura: Boolean,
        falling: Boolean,
    ): Float {
        context.runOnClient<RuntimeException> { ModuleKillAura.enabled = false }
        context.waitFor({ it.player?.onGround() == true }, 200)
        command("kill @e[type=minecraft:cow]")
        context.waitTicks(10)
        command("execute at @p run summon minecraft:cow ~ ~ ~2 {NoAI:1b,PersistenceRequired:1b}")
        context.waitFor({ client ->
            client.level?.entitiesForRendering()?.any { BuiltInRegistries.ENTITY_TYPE.getKey(it.type).path == "cow" && it.isAlive } == true
        }, 200)
        val cow = context.computeOnClient<LivingEntity, RuntimeException> { client ->
            client.level!!.entitiesForRendering().filterIsInstance<LivingEntity>()
                .first { BuiltInRegistries.ENTITY_TYPE.getKey(it.type).path == "cow" && it.isAlive }
        }
        val initialHealth = context.computeOnClient<Float, RuntimeException> { cow.health }
        context.runOnClient<RuntimeException> { ModuleKillAura.enabled = aura }
        context.waitTicks(40)
        if (falling) {
            context.input.holdKeyFor({ it.keyJump }, 2)
            context.waitFor({ client ->
                client.player?.let { !it.onGround() && it.fallDistance > 0.25 && it.deltaMovement.y < 0.0 } == true
            }, 100)
        }
        context.runOnClient<RuntimeException> {
            check(!mc.player!!.isSprinting)
            check(mc.player!!.getAttackStrengthScale(0f) > 0.9f)
            if (aura) {
                check(ModuleKillAura.handleRealAttack())
            } else {
                interaction.attack(mc.player!!, cow)
            }
        }
        context.waitFor({ cow.health < initialHealth }, 100)
        return context.computeOnClient<Float, RuntimeException> { initialHealth - cow.health }
    }
}
