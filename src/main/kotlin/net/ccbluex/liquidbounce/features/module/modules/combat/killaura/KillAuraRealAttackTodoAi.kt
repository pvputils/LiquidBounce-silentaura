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
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot


object KillAuraRealAttackTodoAi {
    var isAttacking = false
        private set
    fun reset() {
        isAttacking = false
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

        if (player.isUsingItem) return

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
        attack()

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

    }

}
