@file:Suppress("MaxLineLength", "LongMethod")

package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventHook
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
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
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRealAttackTodoAi
import net.minecraft.world.phys.EntityHitResult
import kotlin.math.abs

class KillAuraGameTestTodoAi : FabricClientGameTest {
    override fun runTest(context: ClientGameTestContext) {
        context.waitFor({ it.gui.screen() is TitleScreen && LiquidBounce.isInitialized }, 1200)
        context.runOnClient<RuntimeException> {
            check(ModuleManager.map { it.name }.toSet() == setOf("KillAura", "Debug"))
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
            // codex start
            val blockAttacks = AtomicInteger()
            //codex end
            val listener = object : EventListener {}
            context.runOnClient<RuntimeException> {
                EventManager.registerEventHook(AttackEntityEvent::class.java, EventHook<AttackEntityEvent>(listener) {
                    if (it.entity is Zombie) attacks.incrementAndGet()
                })
                // codex start
                EventManager.registerEventHook(BlockAttackEvent::class.java, EventHook<BlockAttackEvent>(listener) {
                    blockAttacks.incrementAndGet()
                })
                //codex end
                check(ModuleKillAura.get().none { it.name in setOf("Clicker", "Criticals", "KeepSprint", "FightBot", "FailSwing", "AutoBlocking", "IgnoreOpenInventory", "SimulateInventoryClosing", "Requires", "AimPoint", "Raycast") }) //codex (check(ModuleKillAura.get().none { it.name in setOf("Clicker", "Criticals", "KeepSprint", "FightBot", "FailSwing", "AutoBlocking", "IgnoreOpenInventory", "SimulateInventoryClosing", "Requires", "AimPoint") }))
                ModuleKillAura.enabled = true
                ConfigSystem.store(ModuleManager.modulesConfig)
                ModuleKillAura.enabled = false
                ConfigSystem.load(ModuleManager.modulesConfig)
                check(ModuleKillAura.enabled) { "Enabled state was not restored from disk" }
            }
            // Explicitly force targets to verify the attack path cannot bypass acquisition checks.
            world.server.runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~6")
            context.waitTicks(10)
            context.runOnClient<RuntimeException> {
                ModuleKillAura.targetTracker.target = mc.level!!.entitiesForRendering().filterIsInstance<Zombie>().first()
            }
            // codex start
            context.input.pressKey { it.keyAttack }
            //codex end
            context.waitTicks(5)
            check(attacks.get() == 0) { "Aura attacked beyond vanilla reach" }
            world.server.runCommand("execute at @p run tp @e[type=minecraft:zombie,limit=1] ~ ~ ~2")
            world.server.runCommand("execute at @p run fill ~-2 ~ ~1 ~2 ~3 ~1 minecraft:stone")
            context.waitTicks(10)
            context.runOnClient<RuntimeException> {
                ModuleKillAura.targetTracker.target = mc.level!!.entitiesForRendering().filterIsInstance<Zombie>().first()
            }
            // codex start
            context.input.pressKey { it.keyAttack }
            //codex end
            context.waitTicks(5)
            check(attacks.get() == 0) { "Aura attacked through a solid wall" }
            // codex start
            check(blockAttacks.get() > 0) { "The aura target swallowed vanilla block-breaking input" }
            //codex end
            world.server.runCommand("execute at @p run fill ~-2 ~ ~1 ~2 ~3 ~1 minecraft:air")
            context.waitFor({ ModuleKillAura.targetTracker.target != null }, 200)
            // codex start
            context.runOnClient<RuntimeException> {
                checkVanillaPickParity()
                mc.player!!.yRot = 90f
                mc.player!!.yRotO = 90f
            }
            //codex end
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
                // codex start
                val vanillaGround = measureDamage(context, world.server::runCommand, false, false)
                val vanillaFalling = measureDamage(context, world.server::runCommand, false, true)
                check(vanillaFalling > vanillaGround) {
                    "Falling baseline did not produce a vanilla critical: ground=$vanillaGround, falling=$vanillaFalling"
                }
                for (timing in listOf("Normal", "Snap", "OnTick")) {
                    context.runOnClient<RuntimeException> {
                        KillAuraRotationsValueGroup.get().first { it.name == "RotationTiming" }.setByString(timing)
                    }
                    val auraGround = measureDamage(context, world.server::runCommand, true, false)
                    val auraFalling = measureDamage(context, world.server::runCommand, true, true)
                    check(abs(vanillaGround - auraGround) < 0.01f) { "$timing grounded damage differs from vanilla" }
                    check(abs(vanillaFalling - auraFalling) < 0.01f) { "$timing critical damage differs from vanilla" }
                }
                //codex end
            } finally {
                context.runOnClient<RuntimeException> {
                    // codex start
                    KillAuraRotationsValueGroup.get().first { it.name == "RotationTiming" }.setByString("Normal")
                    //codex end
                    GlobalSettingsTarget.combat.remove(Targets.PASSIVE)
                    ModuleKillAura.enabled = false
                    ConfigSystem.store(ModuleManager.modulesConfig)
                }
            }
        }
    }

    // codex start
    /** The managed picker must match vanilla at the same angle, including a near miss. */
    private fun checkVanillaPickParity() {
        val player = mc.player!!
        val zombie = mc.level!!.entitiesForRendering().filterIsInstance<Zombie>().first()
        val rotationField = RotationManager::class.java.getDeclaredField("currentRotation").apply { isAccessible = true }
        val savedRotation = rotationField.get(RotationManager)
        val savedHitResult = mc.hitResult
        val aimed = Rotation.lookingAt(zombie.boundingBox.center, player.eyePosition)
        val yaw = player.yRot
        val pitch = player.xRot
        val oldYaw = player.yRotO
        val oldPitch = player.xRotO
        try {
            for (rotation in listOf(aimed, Rotation(aimed.yaw + 35f, aimed.pitch))) {
                val managed = KillAuraRealAttackTodoAi.withVanillaAttack(rotation) {
                    val hit = player.raycastHitResult(1f, player)
                    mc.hitResult = hit
                    true
                }
                check(managed)
                val actual = mc.hitResult!!
                rotationField.set(RotationManager, null)
                player.yRot = rotation.yaw
                player.xRot = rotation.pitch
                player.yRotO = rotation.yaw
                player.xRotO = rotation.pitch
                val expected = player.raycastHitResult(1f, player)
                check(actual.type == expected.type && actual.location.distanceTo(expected.location) < 0.00001)
                check((actual as? EntityHitResult)?.entity === (expected as? EntityHitResult)?.entity)
            }
        } finally {
            player.yRot = yaw
            player.xRot = pitch
            player.yRotO = oldYaw
            player.xRotO = oldPitch
            rotationField.set(RotationManager, savedRotation)
            mc.hitResult = savedHitResult
        }
    }
    //codex end

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
            // codex start
            if (!aura) {
                val rotation = Rotation.lookingAt(cow.boundingBox.center, mc.player!!.eyePosition)
                mc.player!!.yRot = rotation.yaw
                mc.player!!.xRot = rotation.pitch
                mc.player!!.yRotO = rotation.yaw
                mc.player!!.xRotO = rotation.pitch
            }
            //codex end
        }
        // codex start
        context.waitTicks(1)
        context.input.pressKey { it.keyAttack }
        //codex end
        context.waitFor({ cow.health < initialHealth }, 100)
        return context.computeOnClient<Float, RuntimeException> { initialHealth - cow.health }
    }
}
