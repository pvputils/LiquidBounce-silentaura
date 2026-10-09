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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.SNAP
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_ALL
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_NONE
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraRange
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraRangeIndicator
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironment
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.RotationWithVector
import net.ccbluex.liquidbounce.utils.aiming.point.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.preference.LeastDifferencePreference
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.isInventoryOpen
import net.ccbluex.liquidbounce.utils.inventory.isInContainerScreen
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.render.TargetRenderer
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

/**
 * KillAura module
 *
 * Automatically attacks enemies.
 */
@Suppress("MagicNumber")
object ModuleKillAura : ClientModule("KillAura", ModuleCategories.COMBAT) {

    // Attack speed
    val clicker = tree(KillAuraClicker)
    val range = tree(KillAuraRange)
    val targetTracker = tree(KillAuraTargetTracker)

    // Rotation
    private val rotations = tree(KillAuraRotationsValueGroup)
    private val pointTracker = tree(PointTracker(this))

    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")

    private val requirementsMet
        get() = requires.all { it.asBoolean }

    // Bypass techniques
    internal val raycast by enumChoice("Raycast", TRACE_ALL)
    // codex start
    // private val criticalsSelectionMode by enumChoice("Criticals", CriticalsSelectionMode.SMART)
    // codex end
    // codex start
    // private val keepSprint by boolean("KeepSprint", true)
    // codex end

    // Inventory Handling
    // codex start
    // internal val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    // codex end
    // codex start
    // internal val simulateInventoryClosing by boolean("SimulateInventoryClosing", true)
    // codex end

    /**
     * The use of suspend [waitTicks] is a bit too
     * risky for a large and complex module
     * such as KillAura. So back to the basics.
     */
    // codex start
    // internal var waitTicks = 0
    // codex end

    init {
        // codex start
        // tree(KillAuraAutoBlock)
        // codex end
        tree(TargetRenderer(this) {
            targetTracker.target //codex (?.takeUnless { ModuleElytraTarget.isSameTargetRendering(it) })
        })
        // codex start
        // tree(KillAuraFailSwing)
        // codex end
        // codex start
        // tree(KillAuraFightBot)
        // codex end
        tree(KillAuraRangeIndicator)
    }

    override fun onDisabled() {
        targetTracker.reset()
        // codex start
        // failedHits.clear()
        // codex end
        // codex start
        // KillAuraNotifyWhenFail.failedHitsIncrement = 0
        // codex end
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        event.renderEnvironment {
            // codex start
            // renderFailedHits()
            // codex end
            KillAuraRangeIndicator.render(this, event.partialTicks)
        }
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // codex start
        // if (waitTicks > 0) {
        //     waitTicks--
        // }
        // codex end

        // Make sure killaura-logic is not running while inventory is open
        val isInInventoryScreen = isInventoryOpen || mc.gui.screen() is ContainerScreen
        val shouldResetTarget = player.isSpectator || player.isDeadOrDying || !requirementsMet

        if (isInInventoryScreen || shouldResetTarget) { //codex (&& !ignoreOpenInventory)
            // Reset current target
            targetTracker.reset()
            return@handler
        }

        // Update the current target tracker to make sure you attack the best enemy
        updateTarget()

        // Update Auto Weapon
        // codex start
        // ModuleAutoWeapon.onTarget(targetTracker.target)
        // codex end
    }

    @Suppress("unused")
    private val gameHandler = tickHandler {
        if (player.isDeadOrDying || player.isSpectator) {
            return@tickHandler
        }

        // Check if there is target to attack
        val target = targetTracker.target

        if (CombatManager.shouldPauseCombat) {
            // codex start
            // KillAuraAutoBlock.stopBlocking()
            // codex end
            return@tickHandler
        }

        if (target == null) {
            // codex start
            // val hasUnblocked = KillAuraAutoBlock.stopBlocking()
            // codex end

            // Deal with fake swing when there is no target
            // codex start
            // if (KillAuraFailSwing.enabled && requirementsMet) {
            //     // codex start
            //     // if (hasUnblocked && KillAuraAutoBlock.pauseOnUnblockTicks > 0) {
            //     //     waitTicks = KillAuraAutoBlock.pauseOnUnblockTicks
            //     // } else {
            //     //     dealWithFakeSwing(null)
            //     // }
            //     // codex end
            //
            //     dealWithFakeSwing(null)
            //
            // }
            // codex end
            return@tickHandler
        }

        // Check if the module should (not) continue after the blocking state is updated
        if (!requirementsMet) {
            return@tickHandler
        }

        val rotation = (if (rotations.rotationTiming == ON_TICK) {
            findRotation(target, range.interactionRange, range.interactionThroughWallsRange)?.rotation
        } else {
            null
        } ?: RotationManager.currentRotation ?: player.rotation).normalize()

        val crosshairTarget = when {
            raycast != TRACE_NONE -> {
                findEntityInCrosshair(range.interactionRange.toDouble(), rotation, predicate = {
                    // codex start
                    // when (raycast) {
                    //     TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    //     TRACE_ALL -> true
                    //     else -> false
                    // }
                    // codex end
                    it.shouldBeAttacked() //codex (TRACE_ONLYENEMY -> it.shouldBeAttacked())
                })?.entity ?: target
            }
            else -> target
        }

        if (crosshairTarget is LivingEntity && crosshairTarget.shouldBeAttacked() && crosshairTarget != target) {
            targetTracker.target = crosshairTarget
        }

        attackTarget(crosshairTarget, rotation)
    }

    // codex start
    // val shouldBlockSprinting
    //     get() = !ModuleElytraTarget.running
    //         && criticalsSelectionMode.shouldStopSprinting(clicker, targetTracker.target)
    // codex end

    // codex start
    // @Suppress("unused")
    // private val sprintHandler = handler<SprintEvent> { event ->
    //     if (shouldBlockSprinting && (event.source == SprintEvent.Source.MOVEMENT_TICK ||
    //             event.source == SprintEvent.Source.INPUT)) {
    //         event.sprint = false
    //     }
    // }
    // codex end

    @Suppress("CognitiveComplexMethod", "CyclomaticComplexMethod")
    private fun attackTarget(target: Entity, rotation: Rotation) {
        // Make it seem like we are blocking
        // codex start
        // KillAuraAutoBlock.makeSeemBlock()
        // codex end

        debugParameter("Rotation") { rotation }
        debugParameter("Target") { target.scoreboardName }

        val attackHitResult = isLookingAtEntity(
            toEntity = target,
            rotation = rotation,
            range = range.interactionRange.toDouble(),
            throughWallsRange = range.interactionThroughWallsRange.toDouble()
        )

        debugParameter("Target Hit Result") { attackHitResult?.location }

        // codex start
        // val isInRange = ModuleElytraTarget.canIgnoreKillAuraRotations ||
        //     attackHitResult != null && range.isInRange(pos = attackHitResult.location)
        // codex end
        val isInRange = //codex (ModuleElytraTarget.canIgnoreKillAuraRotations ||)
            attackHitResult != null && range.isInRange(pos = attackHitResult.location)
        debugParameter("Is In Range") { isInRange }

        // Check if our target is in range, otherwise deal with auto block
        if (!isInRange) {
            // codex start
            // if (KillAuraAutoBlock.enabled && KillAuraAutoBlock.onScanRange &&
            //     player.squaredBoxedDistanceTo(target) <= range.scanRange.sq()) {
            //     if (KillAuraClicker.ticksSinceLastClick >= KillAuraAutoBlock.reblockTicks) {
            //         KillAuraAutoBlock.startBlocking()
            //     }
            //
            //     return
            // }
            // codex end

            // Make sure we are not blocking
            // codex start
            // val hasUnblocked = KillAuraAutoBlock.stopBlocking()
            // codex end
            // codex start
            // if (hasUnblocked && KillAuraAutoBlock.pauseOnUnblockTicks > 0) {
            //     waitTicks = KillAuraAutoBlock.pauseOnUnblockTicks
            // }else if (KillAuraFailSwing.enabled) {
            //     dealWithFakeSwing(target)
            // }
            // codex end
            // codex start
            // if (KillAuraFailSwing.enabled) { //codex (}else if (KillAuraFailSwing.enabled) {)
            //     dealWithFakeSwing(target)
            // }
            // codex end
            return
        }

        debugParameter("Valid Rotation") { rotation }

        val mainHandStack = player.mainHandItem

        // Attack enemy, according to the attack scheduler
        // codex start
        // if (clicker.isClickTick && canAttackNow(mainHandStack) && //codex (target,)
        //     !KillAuraAutoBlock.isPrioritizingBlocking) {
        // codex end
        if (clicker.isClickTick && //codex (&& !KillAuraAutoBlock.isPrioritizingBlocking)
            canAttackNow(mainHandStack)) { //codex (!KillAuraAutoBlock.isPrioritizingBlocking)
            clicker.prepareForAttack(rotation) {
                // On each click, we check if we are still ready to attack
                if (!canAttackNow(mainHandStack)) { //codex (target,)
                    return@prepareForAttack false
                }

                // Attack enemy
                attackEntity(target, SwingMode.DO_NOT_HIDE) //codex (, keepSprint)
                range.update()
                // codex start
                // KillAuraNotifyWhenFail.failedHitsIncrement = 0
                // codex end
                // codex start
                // KillAuraAutoBlock.hasBlockedSinceAttack = false
                // codex end

                // codex start
                // GenericDebugRecorder.recordDebugInfo(ModuleKillAura, "attackEntity", JsonObject().apply {
                //     add("player", GenericDebugRecorder.debugObject(player))
                //     add("targetPos", GenericDebugRecorder.debugObject(target))
                // })
                // codex end

                true
            }
        }
// codex start
// else if (KillAuraClicker.ticksSinceLastClick >= KillAuraAutoBlock.reblockTicks) {
//             KillAuraAutoBlock.startBlocking()
//         }
// codex end

    }

    private fun updateTarget() {
        // Calculate maximum range based on enemy distance
        val maximumRange = if (targetTracker.closestSquaredEnemyDistance > range.interactionRange.sq()) {
            range.scanRange
        } else {
            range.interactionRange
        }

        debugParameter("Maximum Range") { maximumRange }
        debugParameter("Range") { range }
        val squaredMaxRange = maximumRange.sq()
        val squaredNormalRange = range.interactionRange.sq()

        // Find a suitable target
        val target = targetTracker.targets()
            .filter { entity -> entity.squaredBoxedDistanceTo(player) <= squaredMaxRange }
            .sortedBy { entity -> if (entity.squaredBoxedDistanceTo(player) <= squaredNormalRange) 0 else 1 }
            .firstOrNull { entity -> processTarget(entity, maximumRange, range.interactionThroughWallsRange) }

        if (target != null) {
            targetTracker.target = target
        }
// codex start
// else if (KillAuraFightBot.enabled) {
//             KillAuraFightBot.updateTarget()
//
//             RotationManager.setRotationTarget(
//                 rotations.toRotationTarget(
//                     KillAuraFightBot.getMovementRotation(),
//                     considerInventory = !ignoreOpenInventory
//                 ),
//                 priority = Priority.IMPORTANT_FOR_USAGE_2,
//                 provider = ModuleKillAura
//             )
//         }
// codex end
        else { //codex (} else if (KillAuraFightBot.enabled) {)
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

            // If our click scheduler is not going to click the moment we reach the target,
            // we should not start aiming towards the target just yet.
            SNAP -> if (!clicker.willClickAt(ticks.coerceAtLeast(1))) {
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
                // codex start
                // considerInventory = !ignoreOpenInventory
                // codex end
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
        val point = pointTracker.findPoint(eyes, entity)

        debugGeometry("Box") { ModuleDebug.DebuggedBox(point.box, Color4b.ORANGE.with(a = 90)) }
        debugGeometry("Point") { ModuleDebug.DebuggedPoint(point.pos, Color4b.WHITE, size = 0.1) }

        val rotationPreference = LeastDifferencePreference.leastDifferenceToLastPoint(eyes, point.pos)

        // raytrace to the point
        val rotation = raytraceBox(
            eyes = eyes,
            box = point.box,
            range = range.toDouble(),
            wallsRange = wallsRange.toDouble(),
            rotationPreference = rotationPreference
        )

        return if (rotation == null && rotations.aimThroughWalls) {
            val rotationThroughWalls = raytraceBox(
                eyes = eyes,
                box = point.box,
                // Since [range] is squared, we need to square root
                range = range.toDouble(),
                wallsRange = range.toDouble(),
                rotationPreference = rotationPreference
            )

            rotationThroughWalls
        } else {
            rotation
        }
    }

    /**
     * Check if we can attack the target at the current moment
     */
    internal fun canAttackNow(
        // codex start
        // target: Entity? = null,
        // codex end
        itemStack: ItemStack = player.mainHandItem,
    ): Boolean {
        if (!itemStack.isItemEnabled(world.enabledFeatures())) {
            return false
        }

        if (player.cannotAttackWithItem(itemStack, 0)) {
            return false
        }

        // codex start
        // val criticalHitAllowed = target == null || player.isFallFlying || criticalsSelectionMode.isCriticalHit()
        // if (!criticalHitAllowed) {
        //     return false
        // }
        //
        // codex end
        // codex start
        // val isInventoryBlockingAttack = (isInventoryOpen || isInContainerScreen) &&
        //     !ignoreOpenInventory && !simulateInventoryClosing
        // codex end
        val isInventoryBlockingAttack = (isInventoryOpen || isInContainerScreen) //codex (&& !ignoreOpenInventory)
        return !isInventoryBlockingAttack
    }

    enum class RaycastMode(override val tag: String) : Tagged {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"),
        TRACE_ALL("All")
    }

}
