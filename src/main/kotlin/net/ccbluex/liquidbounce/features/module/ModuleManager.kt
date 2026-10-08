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
package net.ccbluex.liquidbounce.features.module

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.config.types.VALUE_NAME_ORDER
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleMaceKill
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePingSpoof
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleEasyPearl
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTargetLock
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleTeams
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.ModuleNameProtect
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.ModuleLiquidWalk
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleEagle
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleFastUse
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.client.clientStartDurationMs
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.input.InputBind

private val modules = ObjectRBTreeSet<ClientModule>(VALUE_NAME_ORDER)

/**
 * A fairly simple module manager
 */
object ModuleManager : EventListener, Collection<ClientModule> by modules {

    val modulesConfig = ConfigSystem.root("modules", modules)

    private const val SMART_MOUSE_HOLD_THRESHOLD_MS = 200L

    private enum class SmartBindKeyboardState {
        PENDING_ENABLED, PENDING_DISABLED, HOLDING,
    }
    private class SmartBindMouseState(val pendingEnabled: Boolean, val pressTimestamp: Long)

    private val smartKeyboardStates = Reference2ObjectArrayMap<ClientModule, SmartBindKeyboardState>()
    private val smartMouseStates = Reference2ObjectArrayMap<ClientModule, SmartBindMouseState>()

    private fun modulesWithOwnBinds() = modules.filterNot(ClientModule::externalBind)

    /**
     * Handles keystrokes for module binds.
     * This also runs in GUIs, so that if a GUI is opened while a key is pressed,
     * any modules that need to be disabled on key release will be properly disabled.
     */
    @Suppress("unused")
    private val keyboardKeyHandler = handler<KeyboardKeyEvent> { event ->
        if (event.isPressed) {
            if (mc.gui.screen() == null) {
                // Usually nobody actually wants a module to activate when they press the Minecraft debug key combo.
                if (mc.options.keyDebugModifier.isDown) return@handler
                for (m in modulesWithOwnBinds()) {
                    if (!m.bind.matchesKeyPress(event)) {
                        continue
                    }

                    when (m.bind.action) {
                        InputBind.BindAction.TOGGLE -> m.enabled = !m.enabled
                        InputBind.BindAction.HOLD -> m.enabled = true
                        InputBind.BindAction.SMART -> {
                            smartKeyboardStates[m] = if (m.enabled) {
                                SmartBindKeyboardState.PENDING_ENABLED
                            } else {
                                SmartBindKeyboardState.PENDING_DISABLED
                            }
                            m.enabled = true
                        }
                    }
                }
            }
        } else if (event.isRepeat) {
            for (m in modulesWithOwnBinds()) {
                if (m.bind.action != InputBind.BindAction.SMART ||
                    !m.bind.matchesKey(event.scanCode) ||
                    m !in smartKeyboardStates
                ) {
                    continue
                }

                smartKeyboardStates[m] = SmartBindKeyboardState.HOLDING
            }
        } else if (event.isReleased) {
            for (m in modulesWithOwnBinds()) {
                if (!m.bind.matchesKeyRelease(event)) {
                    continue
                }

                when (m.bind.action) {
                    InputBind.BindAction.HOLD -> m.enabled = false

                    InputBind.BindAction.SMART -> {
                        val stateBeforePress = smartKeyboardStates.remove(m) ?: continue
                        m.enabled = stateBeforePress == SmartBindKeyboardState.PENDING_DISABLED
                    }

                    InputBind.BindAction.TOGGLE -> {}
                }
            }
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        if (event.isPressed) {
            if (mc.gui.screen() == null) {
                for (m in modulesWithOwnBinds()) {
                    if (!m.bind.matchesMousePress(event)) {
                        continue
                    }

                    when (m.bind.action) {
                        InputBind.BindAction.TOGGLE -> m.enabled = !m.enabled
                        InputBind.BindAction.HOLD -> m.enabled = true
                        InputBind.BindAction.SMART -> {
                            smartMouseStates[m] = SmartBindMouseState(m.enabled, clientStartDurationMs)
                            m.enabled = true
                        }
                    }
                }
            }
        } else if (event.isReleased) {
            for (m in modulesWithOwnBinds()) {
                if (!m.bind.matchesMouseRelease(event)) {
                    continue
                }

                when (m.bind.action) {
                    InputBind.BindAction.HOLD -> m.enabled = false

                    InputBind.BindAction.SMART -> {
                        val state = smartMouseStates.remove(m) ?: continue

                        // Mouse button events do not emit SDL repeat, so SMART falls back to:
                        // - hold if the press was long enough
                        // - toggle otherwise
                        val shouldFallbackToHold =
                            clientStartDurationMs - state.pressTimestamp >= SMART_MOUSE_HOLD_THRESHOLD_MS

                        if (shouldFallbackToHold) {
                            m.enabled = false
                        } else {
                            m.enabled = !state.pendingEnabled
                        }
                    }

                    InputBind.BindAction.TOGGLE -> {}
                }
            }
        }
    }

    /**
     * Handles world change and enables modules that are not enabled yet
     */
    @Suppress("unused")
    private val handleWorldChange = sequenceHandler<WorldChangeEvent> { event ->
        // Delayed start handling
        if (event.world != null) {
            tickUntil { inGame }
            AutoConfig.withLoading {
                for (module in modules) {
                    if (!module.enabled || module.calledSinceStartup) continue

                    try {
                        module.calledSinceStartup = true
                        // inGame is false here, so use onToggle0
                        module.onToggled(true)
                    } catch (e: Exception) {
                        logger.error("Failed to enable module ${module.name}", e)
                    }
                }
            }
        }

        // Store modules configuration after world change, happens on disconnect as well
        ConfigSystem.store(modulesConfig)
    }

    /**
     * Handles disconnect and if [ClientModule.disableOnQuit] is true disables module
     */
    @Suppress("unused")
    private val handleDisconnect = handler<DisconnectEvent> {
        for (module in modules) {
            if (module.disableOnQuit) {
                try {
                    module.enabled = false
                } catch (e: Exception) {
                    logger.error("Failed to disable module ${module.name}", e)
                }
            }
        }
    }

    /**
     * Register inbuilt client modules
     */
    @Suppress("LongMethod")
    fun registerInbuilt() {
        val builtin = arrayOf(
            // Combat
            ModuleAutoBow,
            ModuleAutoClicker,
            ModuleAutoBuff,
            ModuleAutoWeapon,
            ModuleCriticals,
            ModuleKillAura,
            ModuleVelocity,
            ModuleSwordBlock,
            ModuleMaceKill,

            // Exploit
            ModuleMultiActions,
            ModulePingSpoof,

            // Fun

            // Misc
            ModuleAntiBot,
            ModuleElytraTarget,
            ModuleNameProtect,
            ModuleTeams,
            ModuleTargetLock,
            ModulePacketLogger,
            ModuleDebugRecorder,
            ModuleEasyPearl,

            // Movement
            ModuleFly,
            ModuleFreeze,
            ModuleLiquidWalk,
            ModuleSafeWalk,
            ModuleSpeed,

            // Player
            ModuleOffhand,
            ModuleEagle,
            ModuleFastUse,
            ModuleInventoryCleaner,
            ModuleNoFall,

            // Render
            ModuleCombineMobs,
            ModuleDebug,

            // World
            ModuleCrystalAura,
            ModuleScaffold,
        )

        builtin.forEach { module ->
            addModule(module)
            module.walkKeyPath()
            module.verifyFallbackDescription()
        }
    }

    fun addModule(module: ClientModule) {
        if (!modules.add(module)) {
            error("Module '${module.name}' is already registered.")
        }

        runCatching {
            module.walkInit()
            module.onRegistration()
        }.onFailure {
            modules.remove(module)
        }.getOrThrow()
    }

    fun removeModule(module: ClientModule) {
        // The set compares by name, so check identity.
        check(any { it === module }) { "Module '${module.name}' is not registered." }
        modules.remove(module)

        if (module.enabled) {
            module.enabled = false
        }
        module.unregister()
    }

    fun clear() {
        modules.clear()
    }

    @AddonApi
    operator fun get(moduleName: String) = modules.find { it.name.equals(moduleName, true) }

}
