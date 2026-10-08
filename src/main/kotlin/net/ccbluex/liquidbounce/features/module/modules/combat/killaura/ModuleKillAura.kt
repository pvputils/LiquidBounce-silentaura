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
// codex start
@file:Suppress("MaxLineLength")
//codex end

package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.SNAP
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraRangeTodoAi
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraRangeIndicator
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

/**
 * KillAura module
 *
 * Aims at enemies; attack input follows Minecraft's vanilla pipeline. //codex (Aims at enemies and redirects real attack-button presses.)
 */
@Suppress("MagicNumber")
object ModuleKillAura : ClientModule("KillAura", ModuleCategories.COMBAT) {

    // Real attack input //codex (Attack speed)
    val realAttack = KillAuraRealAttackTodoAi //codex (val clicker = tree(KillAuraClicker))
    val range = KillAuraRangeTodoAi //codex (val range = tree(KillAuraRange))
    val targetTracker = tree(KillAuraTargetTracker)

    // Rotation
    private val rotations = tree(KillAuraRotationsValueGroup)

    init {
        tree(TargetRenderer(this) {
            targetTracker.target //codex (targetTracker.target?.takeUnless { ModuleElytraTarget.isSameTargetRendering(it) })
        })
        tree(KillAuraRangeIndicator)
    }

    override fun onDisabled() {
        // codex start
        realAttack.reset()
        //codex end
        targetTracker.reset()
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            KillAuraRangeIndicator.render(this, event.partialTicks)
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen = isInventoryOpen || mc.gui.screen() is ContainerScreen
        val shouldResetTarget = player.isSpectator || player.isDeadOrDying //codex (val shouldResetTarget = player.isSpectator || player.isDeadOrDying || !requirementsMet)

        if (isInInventoryScreen || shouldResetTarget) { //codex (if (isInInventoryScreen && !ignoreOpenInventory || shouldResetTarget) {)
            // Reset current target
            targetTracker.reset()
            return@handler
        }

        // Update the current target tracker to make sure you attack the best enemy
        updateTarget()

    }

    // codex start
    /** Provides only a rotation. Minecraft chooses and validates the actual click target. */
    fun rotationForAttack(): Rotation? {
        if (!running || mc.gui.screen() != null || player.isSpectator || player.isDeadOrDying
            || CombatManager.shouldPauseCombat) {
            return null
        }
        val target = targetTracker.target ?: return null
        return when (rotations.rotationTiming) {
            ON_TICK, SNAP -> findRotation(target, range.interactionRange, 0f)?.rotation?.normalize()
            else -> RotationManager.currentRotation
        }
    }
    //codex end

    private fun updateTarget() {
        // Calculate maximum range based on enemy distance
        val maximumRange = range.interactionRange //codex (val maximumRange = if (targetTracker.closestSquaredEnemyDistance > range.interactionRange.sq()) range.scanRange else range.interactionRange)

        debugParameter("Maximum Range") { maximumRange }
        debugParameter("Range") { range }
        val squaredMaxRange = maximumRange.sq()
        val squaredNormalRange = range.interactionRange.sq()

        // Find a suitable target
        val target = targetTracker.targets()
            .filter { entity -> entity.squaredBoxedDistanceTo(player) <= squaredMaxRange }
            .sortedBy { entity -> if (entity.squaredBoxedDistanceTo(player) <= squaredNormalRange) 0 else 1 }
            .firstOrNull { entity -> processTarget(entity, maximumRange, 0f) } //codex (.firstOrNull { entity -> processTarget(entity, maximumRange, range.interactionThroughWallsRange) })

        if (target != null) {
            targetTracker.target = target
        } else {
            targetTracker.reset()
        }
    }

    @Suppress("ReturnCount")
    private fun processTarget(
        entity: LivingEntity,
        range: Float,
        wallsRange: Float
    ): Boolean {
        val (rotation, _) = findRotation(entity, range, wallsRange) ?: return false
        val ticks = rotations.calculateTicks(rotation)
        debugParameter("Rotation Ticks") { ticks }

        when (rotations.rotationTiming) {

            // SNAP starts aiming only during a real attack press. //codex (If our click scheduler is not going to click the moment we reach the target, we should not start aiming towards the target just yet.)
            SNAP -> { //codex (SNAP -> if (!realAttack.isAttacking) {)
                return true
            }

            // [ON_TICK] will always instantly aim onto the target on attack, however, if
            // our rotation is unable to be ready in time, we can at least start aiming towards
            // the target.
            ON_TICK -> if (ticks <= 1) {
                return true
            }

            else -> {
                // Continue with regular aiming
            }
        }

        RotationManager.setRotationTarget(
            rotations.toRotationTarget(
                rotation,
                entity,
                considerInventory = true //codex (considerInventory = !ignoreOpenInventory)
            ),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = this@ModuleKillAura
        )
        return true
    }

    /**
     * Get the best spot to attack the entity
     *
     * @param entity The entity to attack
     * @param range The range to attack the entity (NOT SQUARED)
     *
     *  @return The best spot to attack the entity
     */
    private fun findRotation(entity: Entity, range: Float, wallsRange: Float): RotationWithVector? {
        if (rotations.lazyRotation) {
            val currentRotation = RotationManager.currentRotation ?: player.rotation
            val currentHit = isLookingAtEntity(
                fromEntity = player,
                toEntity = entity,
                rotation = currentRotation,
                range = range.toDouble(),
                throughWallsRange = wallsRange.toDouble(),
            )

            if (currentHit != null) {
                debugParameter("Lazy Rotation") { true }
                return RotationWithVector(currentRotation, currentHit.location)
            }
        }

        debugParameter("Lazy Rotation") { false }
        val eyes = player.eyePosition
        val box = entity.boundingBox //codex (val point = pointTracker.findPoint(eyes, entity))


        val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, box.center) //codex (val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, point.pos))

        // raytrace to the point
        val rotation = raytraceBox(
            eyes = eyes,
            box = box, //codex (box = point.box,)
            range = range.toDouble(),
            wallsRange = wallsRange.toDouble(),
            rotationPreference = rotationPreference
        )

        return rotation //codex (return if (rotation == null && rotations.aimThroughWalls) rotationThroughWalls else rotation)
    }

}
