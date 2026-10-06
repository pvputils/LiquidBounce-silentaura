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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.simulateInventoryClosing
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.network.send1_11_1OpenInventory
import net.ccbluex.liquidbounce.utils.network.sendCloseInventory
import net.ccbluex.liquidbounce.utils.entity.isBlockingServerside
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot


object KillAuraRealAttackTodoAi {
    var isAttacking = false
        private set
    private var lastAttackTick = Int.MIN_VALUE

    val ticksSinceLastAttack: Int
        get() = if (lastAttackTick == Int.MIN_VALUE) Int.MAX_VALUE else player.tickCount - lastAttackTick

    fun reset() {
        lastAttackTick = Int.MIN_VALUE
    }

    /** Executes only during vanilla's attack-button press, with no queue, repeats or future click prediction. */
    fun handleInput(attack: () -> Unit) {
        isAttacking = true
        try {
            attack()
        } finally {
            isAttacking = false
        }
    }

    @Suppress("CognitiveComplexMethod")
    fun prepareForAttack(rotation: Rotation? = null, attack: () -> Boolean) {
        if (!isAttacking) {
            // If we are not going to click, we don't need to prepare the environment
            return
        }

        // 1. Stop blocking
        if (player.isBlockingServerside || KillAuraAutoBlock.enforcedBlockingHand != null) {
            if (!KillAuraAutoBlock.enabled && !ModuleMultiActions.mayAttackWhileUsing()) {
                return
            }

            if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.shouldUnblockToHit) {
                if (KillAuraAutoBlock.stopBlocking(pauses = true) && KillAuraAutoBlock.pauseOnUnblockTicks > 0) {
                    ModuleKillAura.waitTicks = KillAuraAutoBlock.pauseOnUnblockTicks
                    return
                }
            }
        } else if (player.isUsingItem && !ModuleMultiActions.mayAttackWhileUsing()) {
            // Since we are not allowed to attack while the player is using another item,
            // we will return here.
            return
        }

        val wasSimulatedInventoryClose = simulateInventoryClosing && InventoryManager.isInventoryOpen

        // 2. Close Inventory
        if (wasSimulatedInventoryClose) {
            network.sendCloseInventory()
        }

        // 3. Rotate to target (if we have on-tick enabled)
        if (rotationTiming != KillAuraRotationsValueGroup.KillAuraRotationTiming.NORMAL && rotation != null) {
            network.send(
                PosRot(
                    player.x,
                    player.y,
                    player.z,
                    rotation.yaw,
                    rotation.pitch,
                    player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        // Run the attack
        if (attack()) lastAttackTick = player.tickCount

        // 1. Rotate back
        if (rotationTiming != KillAuraRotationsValueGroup.KillAuraRotationTiming.NORMAL && rotation != null) {
            network.send(
                PosRot(
                    player.x,
                    player.y,
                    player.z,
                    player.withFixedYaw(rotation),
                    player.xRot,
                    player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        // 2. Start blocking again
        if (KillAuraAutoBlock.blockImmediate) {
            KillAuraAutoBlock.startBlocking()
        }

        // 3. Open inventory again
        if (wasSimulatedInventoryClose) {
            network.send1_11_1OpenInventory()
        }
    }

}
