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

@file:Suppress("MatchingDeclarationName")

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.unmodifiable

@Tag("scheduleInventoryAction")
class ScheduleInventoryActionEvent(val schedule: MutableList<InventoryAction.Chain> = mutableListOf()) : Event() {

    fun schedule(
        constrains: InventoryConstraints,
        action: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, listOf(action), priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        vararg actions: InventoryAction,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, actions.unmodifiable(), priority))
    }

    fun schedule(
        constrains: InventoryConstraints,
        actions: List<InventoryAction>,
        priority: Priority = Priority.NORMAL
    ) {
        this.schedule.add(InventoryAction.Chain(constrains, actions, priority))
    }
}

