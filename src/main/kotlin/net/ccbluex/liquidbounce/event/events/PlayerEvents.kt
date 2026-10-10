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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.minecraft.world.entity.MoverType
import net.minecraft.world.phys.Vec3

// Entity events bound to client-user entity
// codex start
// @Tag("healthUpdate")
// class HealthUpdateEvent(val health: Float, val food: Int, val saturation: Float, val previousHealth: Float) : Event()
// // codex start
// //
// // @Tag("death")
// // object DeathEvent : Event(), WebSocketEvent
// // codex end
// codex end
// codex start
//
// @AddonApi
// @Tag("playerTick")
// class PlayerTickEvent : CancellableEvent()
// // codex start
// //
// // @AddonApi
// // @Tag("playerPostTick")
// // object PlayerPostTickEvent : Event()
// // // codex start
// // //
// // // @Tag("playerMovementTick")
// // // class PlayerMovementTickEvent : CancellableEvent()
// // // codex end
// // // codex start
// // //
// // // @AddonApi
// // // @Tag("playerNetworkMovementTick")
// // // class PlayerNetworkMovementTickEvent(
// // //     val state: EventState,
// // //     var x: Double,
// // //     var y: Double,
// // //     var z: Double,
// // //     var ground: Boolean
// // // ) : CancellableEvent()
// // // // codex start
// // // //
// // // // @Tag("playerPushOut")
// // // // class PlayerPushOutEvent : CancellableEvent()
// // // // codex end
// // // codex end
// // codex end
// codex end

@AddonApi
@Tag("playerMove")
class PlayerMoveEvent(val type: MoverType, var movement: Vec3) : Event()
// codex start
//
// @AddonApi
// @Tag("playerJump")
// class PlayerJumpEvent(var motion: Float, var yaw: Float) : CancellableEvent()
// // codex start
// //
// // @Tag("playerAfterJump")
// // object PlayerAfterJumpEvent : Event()
// // codex end
// codex end

@Tag("playerUseMultiplier")
class PlayerUseMultiplier(var forward: Float, var sideways: Float) : Event()

@Tag("playerSneakMultiplier")
class PlayerSneakMultiplier(var multiplier: Double) : Event()

/**
 * Warning: UseHotbarSlotOrOffHand won't stimulate this event
 */
// codex start
// @Tag("playerInteractItem")
// class PlayerInteractItemEvent(val player: Player, val hand: InteractionHand) : CancellableEvent()
// codex end
// codex start
//
// @Tag("playerInteractedItem")
// class PlayerInteractedItemEvent(
//     val player: Player,
//     val hand: InteractionHand,
//     val actionResult: InteractionResult,
// ) : Event()
// codex end

@Tag("playerStrafe")
class PlayerVelocityStrafe(val movementInput: Vec3, val speed: Float, val yaw: Float, var velocity: Vec3) : Event()
// codex start
//
// @Tag("playerStride")
// class PlayerStrideEvent(var strideForce: Float) : Event()
// codex end

@Tag("playerSafeWalk")
class PlayerSafeWalkEvent(var isSafeWalk: Boolean = false) : Event()
// codex start
//
// @Tag("playerStep")
// class PlayerStepEvent(var height: Float) : Event()
// codex end
// codex start
//
// @Tag("playerStepSuccess")
// class PlayerStepSuccessEvent(val movementVec: Vec3, var adjustedVec: Vec3) : Event()
// // codex start
// //
// // @Tag("playerFluidCollisionCheck")
// // class PlayerFluidCollisionCheckEvent(val fluid: TagKey<Fluid>) : CancellableEvent()
// // // codex start
// // //
// // // @Tag("playerContainerInput")
// // // class PlayerContainerInputEvent(
// // //     val containerId: Int,
// // //     val slot: Int,
// // //     val button: Int,
// // //     val input: ContainerInput,
// // // ) : CancellableEvent()
// // // codex end
// // codex end
// codex end
