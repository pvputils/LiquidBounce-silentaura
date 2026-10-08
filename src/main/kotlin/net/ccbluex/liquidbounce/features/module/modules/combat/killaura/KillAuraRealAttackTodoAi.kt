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

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot

object KillAuraRealAttackTodoAi {
    // codex start
    var attackRotation: Rotation? = null
        private set

    fun reset() {
        attackRotation = null
    }

    /** Scope rotation only; the callback runs vanilla picking and the complete vanilla click. */
    fun withVanillaAttack(rotation: Rotation, attack: () -> Boolean): Boolean {
        val previous = attackRotation
        val restoreRotation = RotationManager.currentRotation ?: Rotation(player.yRot, player.xRot)
        val yaw = player.yRot
        val pitch = player.xRot
        val oldYaw = player.yRotO
        val oldPitch = player.xRotO
        attackRotation = rotation
        player.yRot = rotation.yaw
        player.xRot = rotation.pitch
        player.yRotO = rotation.yaw
        player.xRotO = rotation.pitch
        try {
            sendRotation(rotation)
            return attack()
        } finally {
            player.yRot = yaw
            player.xRot = pitch
            player.yRotO = oldYaw
            player.xRotO = oldPitch
            attackRotation = previous
            sendRotation(restoreRotation)
        }
    }

    private fun sendRotation(rotation: Rotation) {
        network.send(
            Rot(
                rotation.yaw, rotation.pitch,
                player.onGround(), player.horizontalCollision
            )
        )
    }
    //codex end
}
