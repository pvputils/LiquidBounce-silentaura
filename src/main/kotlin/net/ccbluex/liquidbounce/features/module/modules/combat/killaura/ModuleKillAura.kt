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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup.KillAuraRotationTiming.SNAP
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_ALL
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_NONE
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_ONLYENEMY
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraRangeTodoAi
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
 * Aims at enemies and redirects real attack-button presses. //codex (Automatically attacks enemies.)
 */
@Suppress("MagicNumber")
object ModuleKillAura : ClientModule("KillAura", ModuleCategories.COMBAT) {

    // Real attack input //codex (Attack speed)
    val realAttack = KillAuraRealAttackTodoAi //codex (val clicker = tree(KillAuraClicker))
    val range = KillAuraRangeTodoAi //codex (val range = tree(KillAuraRange))
    val targetTracker = tree(KillAuraTargetTracker)

    // Rotation
    private val rotations = tree(KillAuraRotationsValueGroup)
    private val pointTracker = tree(PointTracker(this))

    private val requires by multiEnumChoice<KillAuraRequirements>("Requires")

    private val requirementsMet
        get() = requires.all { it.asBoolean }

    // Bypass techniques
    internal val raycast by enumChoice("Raycast", TRACE_ALL)

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
        val shouldResetTarget = player.isSpectator || player.isDeadOrDying || !requirementsMet

        if (isInInventoryScreen || shouldResetTarget) { //codex (if (isInInventoryScreen && !ignoreOpenInventory || shouldResetTarget) {)
            // Reset current target
            targetTracker.reset()
            return@handler
        }

        // Update the current target tracker to make sure you attack the best enemy
        updateTarget()

    }

    // codex start
    @Suppress("CognitiveComplexMethod")
    //codex end
    private fun performRealAttack() { //codex (private val gameHandler = tickHandler {)
        if (player.isDeadOrDying || player.isSpectator) {
            return //codex (return@tickHandler)
        }

        // Check if there is target to attack
        val target = targetTracker.target

        if (CombatManager.shouldPauseCombat) {
            return //codex (return@tickHandler)
        }

        // codex start
        if (target == null) {
            return
        }
        //codex end

        // Check if the module should (not) continue after the blocking state is updated
        if (!requirementsMet) {
            return //codex (return@tickHandler)
        }

        val rotation = (if (rotations.rotationTiming == ON_TICK || rotations.rotationTiming == SNAP) { //codex (val rotation = (if (rotations.rotationTiming == ON_TICK) {)
            findRotation(target, range.interactionRange, 0f)?.rotation //codex (findRotation(target, range.interactionRange, range.interactionThroughWallsRange)?.rotation)
        } else {
            null
        } ?: RotationManager.currentRotation ?: player.rotation).normalize()

        val crosshairTarget = when {
            raycast != TRACE_NONE -> {
                findEntityInCrosshair(range.interactionRange.toDouble(), rotation, predicate = {
                    when (raycast) {
                        TRACE_ONLYENEMY -> it.shouldBeAttacked()
                        TRACE_ALL -> true
                        else -> false
                    }
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
    fun handleRealAttack(): Boolean {
        if (!running || mc.gui.screen() != null || player.isSpectator || player.isDeadOrDying) return false
        // Leave block breaking and attacks without an aura target to vanilla.
        if (targetTracker.target == null) return false
        realAttack.handleInput { performRealAttack() }
        return true
    }
    //codex end

    @Suppress("CognitiveComplexMethod", "CyclomaticComplexMethod")
    private fun attackTarget(target: Entity, rotation: Rotation) {
        // Make it seem like we are blocking

        debugParameter("Rotation") { rotation }
        debugParameter("Target") { target.scoreboardName }

        val attackHitResult = isLookingAtEntity(
            toEntity = target,
            rotation = rotation,
            range = range.interactionRange.toDouble(),
            throughWallsRange = 0.0 //codex (throughWallsRange = range.interactionThroughWallsRange.toDouble())
        )

        debugParameter("Target Hit Result") { attackHitResult?.location }

        val isInRange = attackHitResult != null && range.isInRange(pos = attackHitResult.location) //codex (val isInRange = ModuleElytraTarget.canIgnoreKillAuraRotations || attackHitResult != null && range.isInRange(pos = attackHitResult.location))
        debugParameter("Is In Range") { isInRange }

        // codex start
        if (!isInRange) return
        //codex end

        debugParameter("Valid Rotation") { rotation }

        val mainHandStack = player.mainHandItem

        // Attack once during the current vanilla attack-button press. //codex (Attack enemy, according to the attack scheduler)
        if (realAttack.isAttacking && canAttackNow(mainHandStack)) { //codex (if (realAttack.isAttacking && canAttackNow(mainHandStack) && !KillAuraAutoBlock.isPrioritizingBlocking) {)
            realAttack.prepareForAttack(rotation) { //codex (clicker.prepareForAttack(rotation) {)
                // On each click, we check if we are still ready to attack
                if (!canAttackNow(mainHandStack)) { //codex (if (!canAttackNow(target, mainHandStack)) {)
                    return@prepareForAttack false
                }

                // Attack enemy
                attackEntity(target, SwingMode.DO_NOT_HIDE) //codex (attackEntity(target, SwingMode.DO_NOT_HIDE, keepSprint && !shouldBlockSprinting))



                true
            }
        }
    }

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
            SNAP -> if (!realAttack.isAttacking) { //codex (SNAP -> if (!clicker.willClickAt(ticks.coerceAtLeast(1))) {)
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

        return rotation //codex (return if (rotation == null && rotations.aimThroughWalls) rotationThroughWalls else rotation)
    }

    /**
     * Check if we can attack the target at the current moment
     */
    internal fun canAttackNow(
        itemStack: ItemStack = player.mainHandItem,
    ): Boolean {
        if (!itemStack.isItemEnabled(world.enabledFeatures())) {
            return false
        }

        if (player.cannotAttackWithItem(itemStack, 0)) {
            return false
        }

        return !(isInventoryOpen || isInContainerScreen) //codex (return !isInventoryBlockingAttack)
    }

    enum class RaycastMode(override val tag: String) : Tagged {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"),
        TRACE_ALL("All")
    }

}
