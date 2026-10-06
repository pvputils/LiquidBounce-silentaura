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
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

@Tag("perspective")
object PerspectiveEvent : Event() {
    var perspective: CameraType = CameraType.FIRST_PERSON
    var distance: Float = 0f
    var noClip: Boolean = false

    var lastPerspective: CameraType = CameraType.FIRST_PERSON
    var lastDistance: Float = 0f

    fun update(mc: Minecraft, entity: Entity?) {
        lastDistance = distance
        lastPerspective = perspective

        perspective = mc.options.cameraType
        noClip = false
        distance = entity.cameraDistance
    }
}
