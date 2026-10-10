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
import net.ccbluex.liquidbounce.event.CancellableEvent
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos

@AddonApi
@Tag("worldChange")
class WorldChangeEvent(val world: ClientLevel?) : Event()
// codex start
//
// @Tag("chunkUnload")
// class ChunkUnloadEvent(val pos: ChunkPos) : Event()
// // codex start
// //
// // @AddonApi
// // @Tag("chunkLoad")
// // class ChunkLoadEvent(val x: Int, val z: Int) : Event()
// // codex end
// codex end
// codex start
//
// @Tag("chunkDeltaUpdate")
// class ChunkDeltaUpdateEvent(val packet: ClientboundSectionBlocksUpdatePacket) : Event()
// codex end
// codex start
//
// @AddonApi
// @Tag("blockChange")
// class BlockChangeEvent(val blockPos: BlockPos, val newState: BlockState) : Event()
// // codex start
// //
// // @Tag("blockShape")
// // class BlockShapeEvent(var state: BlockState, var pos: BlockPos, var shape: VoxelShape) : Event()
// // codex end
// codex end
// codex start
//
// @Tag("blockBreakingProgress")
// class BlockBreakingProgressEvent(val pos: BlockPos) : Event()
// codex end

// codex start
// @Tag("blockAttack")
// class BlockAttackEvent(val pos: BlockPos) : CancellableEvent()
// codex end
// codex start
//
// @Tag("blockVelocityMultiplier")
// class BlockVelocityMultiplierEvent(val block: Block, var multiplier: Float) : Event()
// codex end
// codex start
//
// @Tag("blockSlipperinessMultiplier")
// class BlockSlipperinessMultiplierEvent(val block: Block, var slipperiness: Float) : Event()
// codex end
// codex start
//
// @Tag("entityEquipmentChange")
// class EntityEquipmentChangeEvent(
//     val entity: LivingEntity, val equipmentSlot: EquipmentSlot, val itemStack: ItemStack
// ) : Event()
// // codex start
// //
// // @Tag("fluidPush")
// // class FluidPushEvent : CancellableEvent()
// // codex end
// codex end
// codex start
//
// @AddonApi
// @Tag("worldEntityRemove")
// class WorldEntityRemoveEvent(val entity: Entity, val reason: Entity.RemovalReason) : Event()
// codex end
