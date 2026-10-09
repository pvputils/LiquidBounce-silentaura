@file:Suppress("MaxLineLength")

package net.ccbluex.liquidbounce.gametest

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
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
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import com.mojang.authlib.GameProfile
import java.util.UUID

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
            checkAimOnly(context)
        }
    }

    private fun checkAimOnly(context: ClientGameTestContext) {
        var initialYaw = 0f
        context.runOnClient<RuntimeException> {
            val target = RemotePlayer(mc.level!!, GameProfile(UUID.randomUUID(), "AimTargetTodoAi"))
            target.id = 2000000
            target.setPos(mc.player!!.x + 2.0, mc.player!!.y, mc.player!!.z + 2.0)
            mc.level!!.addEntity(target)
            mc.player!!.yRot = 90f
            mc.player!!.xRot = 0f
            initialYaw = mc.player!!.yRot
            KillAuraRotationsValueGroup.get().first { it.name == "MovementCorrection" }.setByString("ChangeLook")
            check(ModuleKillAura.get().none { it.name in setOf("Clicker", "CPS", "Bind", "Raycast") })
            AimOnlyProbeTodoAi.attackPackets = 0
            ModuleKillAura.enabled = true
        }
        context.waitFor({ ModuleKillAura.targetTracker.target is RemotePlayer }, 200)
        context.waitTicks(60)
        context.runOnClient<RuntimeException> {
            check(kotlin.math.abs(mc.player!!.yRot - initialYaw) > 5f) { "Visible aiming did not turn the player" }
            check(AimOnlyProbeTodoAi.attackPackets == 0) { "Aim-only KillAura sent an automatic attack" }
            check(!mc.options.keyAttack.isDown) { "Aim-only KillAura simulated attack input" }
            ModuleKillAura.enabled = false
            ConfigSystem.store(ModuleManager.modulesConfig)
        }
    }
}

private object AimOnlyProbeTodoAi : EventListener {
    var attackPackets = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ServerboundAttackPacket) attackPackets++
    }
}
