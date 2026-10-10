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
@file:Suppress("TooManyFunctions", "WildcardImport")

package net.ccbluex.liquidbounce.utils.inventory

import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
// codex start
//
// fun hasInventorySpace() = player.inventory.nonEquipmentItems.any { it.isEmpty }
// codex end
// codex start
//
// fun findEmptyStorageSlotsInInventory(): List<ItemSlot> {
//     return (Slots.Inventory + Slots.Hotbar).filter { it.itemStack.isEmpty }
// }
// codex end
// codex start
//
// fun findNonEmptyStorageSlotsInInventory(): List<ItemSlot> {
//     return (Slots.Inventory + Slots.Hotbar).filter { !it.itemStack.isEmpty }
// }
// codex end
// codex start
//
// fun findNonEmptySlotsInInventory(): List<ItemSlot> {
//     return Slots.All.filter { !it.itemStack.isEmpty }
// }
//
// /**
// * Exact total capacity of this iterable to store [itemStack] (empty slots count as [ItemStack.maxStackSize],
// mergeable
// * slots as their remaining space). Contract: the slot currently holding [itemStack] must NOT be part of this
// iterable,
//  * otherwise its own remaining capacity would be double-counted and the result overestimated.
//  */
// codex end
// codex start
// @JvmOverloads
// fun Iterable<ItemSlot>.mergeableCapacityFor(itemStack: ItemStack, blacklist: Collection<ItemSlot>? = null): Int =
//     sumOf {
//         val targetStack = it.itemStack
//         when {
//             !blacklist.isNullOrEmpty() && it in blacklist -> 0
//             targetStack.isEmpty -> itemStack.maxStackSize
//             targetStack.isMergeable(itemStack) -> targetStack.maxStackSize - targetStack.count
//             else -> 0
//         }
//     }
// codex end
// codex start
//
// fun AbstractContainerScreen<*>.getSlotsInContainer(): List<ContainerItemSlot> =
//     this.menu.slots
//         .filter { it.container !== player.inventory }
//         .map { ContainerItemSlot(it.index) }
// // codex start
// //
// // fun AbstractContainerScreen<*>.findItemsInContainer(): List<ContainerItemSlot> =
// //     this.menu.slots
// //         .filter { !it.item.isEmpty && it.container !== player.inventory }
// //         .map { ContainerItemSlot(it.index) }
// // codex end
//
// // codex start
// // @AddonApi
// // @JvmOverloads
// // context(requester: EventListener)
// // codex end
// // codex start
// // fun useHotbarSlotOrOffhand(
// //     slot: HotbarItemSlot,
// //     ticksUntilReset: Int = 1,
// //     yRot: Float = RotationManager.currentRotation?.yRot ?: player.yRot,
// //     xRot: Float = RotationManager.currentRotation?.xRot ?: player.xRot,
// //     swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
// // ): InteractionResult {
// //     SilentHotbar.selectSlotSilently(requester, slot, ticksUntilReset)
// //     return net.ccbluex.liquidbounce.utils.entity.useItem(slot.useHand, yRot, xRot, swingMode)
// // }
// // codex end
// // codex start
// //
// // internal fun findBlocksEndingWith(vararg targets: String): SortedSet<Block> =
// //     BuiltInRegistries.BLOCK.filterTo(blockSortedSetOf()) { block ->
// //         targets.any { BuiltInRegistries.BLOCK.getKey(block).path.endsWith(it.lowercase()) }
// //     }
// // codex end
// codex end

val AbstractContainerMenu.typeOrNull: MenuType<*>?
    get() = try { type } catch (_: UnsupportedOperationException) { null }

/**
 * Finds the best slot in this iterable for mining [blockState] using `mc.player` as baseline.
 *
 * The result depends on current player context (e.g. creative state and durability filtering),
 * then ranks candidates by destroy speed and nearby-slot preference.
 */
// codex start
// fun <T : ItemSlot> Iterable<T>.findBestToolToMineBlock(
//     blockState: BlockState,
//     ignoreDurability: Boolean = true,
//     predicate: BiPredicate<ItemStack, BlockState> = BiPredicate { _, _ -> true },
// ): T? {
//     val player = mc.player ?: return null
//
//     val candidates = filter {
//         val stack = it.itemStack
//         val durabilityCheck = (ignoreDurability || (stack.durability > 2 || stack.maxDamage <= 0))
//         !player.isCreative && durabilityCheck && predicate.test(stack, blockState)
//     }
//
//     if (candidates.size > 1) {
//         return candidates.maxWith(
//             Comparator.comparingDouble<T> {
//                 it.itemStack.getDestroySpeedWithEnchantment(blockState).toDouble()
//             }.thenDescending(ItemSlot.PREFER_NEARBY)
//         )
//     }
//
//     return candidates.firstOrNull()
// }
// codex end
