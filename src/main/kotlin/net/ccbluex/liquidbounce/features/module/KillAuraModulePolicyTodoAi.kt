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
@file:Suppress("MaxLineLength")

package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.event.EventListener
import java.util.Collections
import java.util.IdentityHashMap

/** Restricts compatibility classes retained for shared utilities and mixins. */
object KillAuraModulePolicyTodoAi {
    private val names = setOf("KillAura", "AutoWeapon", "AntiBot", "Teams", "TargetLock", "Debug", "SwordBlock", "ElytraTarget", "MultiActions")

    fun allows(name: String) = name in names

    fun allowsListener(listener: EventListener): Boolean {
        val seen = Collections.newSetFromMap(IdentityHashMap<EventListener, Boolean>())
        var current: EventListener? = listener
        while (current != null && seen.add(current)) {
            if (current is ClientModule) return allows(current.name)
            current = current.parent()
        }
        return true
    }
}
