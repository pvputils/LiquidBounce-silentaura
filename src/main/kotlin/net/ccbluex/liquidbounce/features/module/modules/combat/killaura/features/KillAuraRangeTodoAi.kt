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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.AttackRange
import net.minecraft.world.phys.Vec3

/** Uses the actual held item's vanilla range without adjustments or wall bypass. */
object KillAuraRangeTodoAi : MinecraftShortcuts {
    fun getAttackRange(itemStack: ItemStack = player.mainHandItem): AttackRange =
        itemStack.get(DataComponents.ATTACK_RANGE) ?: AttackRange.defaultFor(player)

    val interactionRange: Float
        get() = getAttackRange().effectiveMaxRange(player)

    fun isInRange(itemStack: ItemStack = player.mainHandItem, pos: Vec3): Boolean =
        getAttackRange(itemStack).isInRange(player, pos)
}
