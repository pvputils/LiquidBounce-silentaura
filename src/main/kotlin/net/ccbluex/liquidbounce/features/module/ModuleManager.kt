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
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
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
            // codex start
            // ModuleAimbot,
            // ModuleAutoArmor,
            // codex end
            // codex start
            // ModuleAutoBow,
            // codex end
            // codex start
            // ModuleAutoClicker,
            // codex end
            // codex start
            // ModuleAutoLeave,
            // codex end
            // codex start
            // ModuleAutoBuff,
            // codex end
            // codex start
            // ModuleAutoRod,
            // codex end
            // codex start
            // ModuleAutoWeapon,
            // codex end
            // codex start
            // ModuleFakeLag,
            // codex end
            // codex start
            // ModuleCriticals,
            // codex end
            // codex start
            // ModuleHitbox,
            // codex end
            ModuleKillAura,
            // codex start
            // ModuleTpAura,
            // ModuleSuperKnockback,
            // ModuleTimerRange,
            // ModuleTickBase,
            // codex end
            // codex start
            // ModuleVelocity,
            // codex end
            // codex start
            // ModuleBacktrack,
            // codex end
            // codex start
            // ModuleSwordBlock,
            // codex end
            // codex start
            // ModuleAutoShoot,
            // ModuleKeepSprint,
            // codex end
            // codex start
            // ModuleMaceKill,
            // codex end
            // codex start
            // ModuleSpearKill,
            // ModuleNoMissCooldown,
            // codex end

            // Exploit
            // codex start
            // ModuleAbortBreaking,
            // ModuleAntiReducedDebugInfo,
            // ModuleAntiHunger,
            // ModuleClip,
            // ModuleExtendedFirework,
            // ModuleResetVL,
            // ModuleDamage,
            // ModuleDisabler,
            // ModuleGhostHand,
            // ModuleKick,
            // ModuleMoreCarry,
            // codex end
            // codex start
            // ModuleMultiActions,
            // codex end
            // codex start
            // ModuleNewChunks,
            // ModuleNameCollector,
            // ModuleNoPitchLimit,
            // codex end
            // codex start
            // ModulePingSpoof,
            // codex end
            // codex start
            // ModulePlugins,
            // ModulePortalMenu,
            // ModuleSleepWalker,
            // ModuleVehicleOneHit,
            // ModuleServerCrasher,
            // ModuleDupe,
            // ModuleClickTp,
            // ModuleTimeShift,
            // ModuleTeleport,
            // ModulePhase,
            // ModuleYggdrasilSignatureFix,
            // codex end

            // Fun
            // codex start
            // ModuleDankBobbing,
            // ModuleDerp,
            // ModuleNotebot,
            // ModuleSkinDerp,
            // ModuleHandDerp,
            // ModuleTwerk,
            // ModuleVomit,
            // codex end

            // Misc
            // codex start
            // ModuleAutoConfig,
            // ModuleGUICloser,
            // ModuleBookBot,
            // codex end
            // codex start
            // ModuleAntiBot,
            // codex end
            // codex start
            // ModuleBetterTab,
            // ModuleItemScroller,
            // ModuleBetterChat,
            // codex end
            // codex start
            // ModuleElytraTarget,
            // codex end
            // codex start
            // ModuleMacros,
            // ModuleMiddleClickAction,
            // ModuleInventoryTracker,
            // codex end
            // codex start
            // ModuleNameProtect,
            // codex end
            // codex start
            // ModuleTextFieldProtect,
            // ModuleNotifier,
            // ModuleSpammer,
            // ModuleAutoAccount,
            // codex end
            // codex start
            // ModuleTeams,
            // codex end
            // codex start
            // ModuleElytraSwap,
            // ModuleAutoChatGame,
            // ModuleReportHelper,
            // codex end
            // codex start
            // ModuleTargetLock,
            // codex end
            // codex start
            // ModuleAutoPearl,
            // ModuleAntiStaff,
            // ModuleFlagCheck,
            // codex end
            ModulePacketLogger,
            ModuleDebugRecorder,
            // codex start
            // ModuleAntiCheatDetect,
            // codex end
            // codex start
            // ModuleEasyPearl,
            // codex end

            // Movement
            // codex start
            // ModuleAirJump,
            // ModuleAntiBounce,
            // ModuleAntiLevitation,
            // ModuleAutoDodge,
            // ModuleAvoidHazards,
            // ModuleBlockBounce,
            // ModuleBlockWalk,
            // ModuleElytraRecast,
            // ModuleElytraFly,
            // codex end
            // codex start
            // ModuleFly,
            // codex end
            // codex start
            // ModuleFreeze,
            // codex end
            // codex start
            // ModuleHighJump,
            // ModuleInventoryMove,
            // codex end
            // codex start
            // ModuleLiquidWalk,
            // codex end
            // codex start
            // ModuleLongJump,
            // ModuleNoClip,
            // ModuleNoJumpDelay,
            // ModuleNoPose,
            // ModuleNoPush,
            // ModuleNoSlow,
            // ModuleNoWeb,
            // ModuleParkour,
            // ModuleEntityControl,
            // codex end
            // codex start
            // ModuleSafeWalk,
            // codex end
            // codex start
            // ModuleSneak,
            // codex end
            // codex start
            // ModuleSpeed,
            // codex end
            // codex start
            // ModuleSprint,
            // ModuleStep,
            // ModuleReverseStep,
            // ModuleStrafe,
            // ModuleTerrainSpeed,
            // ModuleTridentBoost,
            // ModuleVehicleBoost,
            // ModuleVehicleControl,
            // ModuleSpider,
            // ModuleTargetStrafe,
            // ModuleAnchor,
            // ModuleSnapTap,
            // codex end

            // Player
            // codex start
            // ModuleAntiVoid,
            // ModuleAntiAFK,
            // ModuleAntiExploit,
            // ModuleAutoBreak,
            // ModuleAutoCrafter,
            // ModuleAutoFish,
            // ModuleAutoRespawn,
            // ModuleAutoWindCharge,
            // codex end
            // codex start
            // ModuleOffhand,
            // codex end
            // codex start
            // ModuleAutoShop,
            // ModuleAutoWalk,
            // ModuleBlink,
            // ModuleChestCleaner,
            // ModuleChestStealer,
            // ModuleAutoDeposit,
            // codex end
            // codex start
            // ModuleEagle,
            // codex end
            // codex start
            // ModuleFastExp,
            // codex end
            // codex start
            // ModuleFastUse,
            // codex end
            // codex start
            // ModuleInventoryCleaner,
            // codex end
            // codex start
            // ModuleNoBlockInteract,
            // ModuleNoEntityInteract,
            // codex end
            // codex start
            // ModuleNoFall,
            // codex end
            // codex start
            // ModuleNoRotateSet,
            // ModuleNoSlotSet,
            // ModuleReach,
            // ModuleAutoQueue,
            // ModuleSmartEat,
            // ModuleReplenish,
            // ModulePotionSpoof,
            // codex end

            // Render
            // codex start
            // ModuleAnimations,
            // ModuleAntiBlind,
            // ModuleBetterInventory,
            // ModuleBlockESP,
            // ModuleBlockOutline,
            // ModuleBreadcrumbs,
            // ModuleCameraClip,
            // ModuleClickGui,
            // ModuleDamageParticles,
            // ModuleParticles,
            // ModuleESP,
            // ModuleLogoffSpot,
            // ModuleFreeCam,
            // ModuleSmoothCamera,
            // ModuleFreeLook,
            // ModuleFullBright,
            // ModuleHoleESP,
            // ModuleHud,
            // ModuleHats,
            // ModuleItemESP,
            // ModuleItemTags,
            // ModuleJumpEffect,
            // ModuleMobOwners,
            // ModuleMurderMystery,
            // ModuleHitFX,
            // ModuleNametags,
            // codex end
            // codex start
            // ModuleCombineMobs,
            // codex end
            // codex start
            // ModuleAspect,
            // ModuleAutoF5,
            // ModuleChams,
            // ModuleBedPlates,
            // ModuleNoBob,
            // ModuleNoFov,
            // ModuleNoHurtCam,
            // ModuleNoSwing,
            // ModuleCustomAmbience,
            // ModuleProphuntESP,
            // ModuleQuickPerspectiveSwap,
            // ModuleRadar,
            // ModuleRotations,
            // ModuleSilentHotbar,
            // ModuleStorageESP,
            // ModuleTNTTimer,
            // ModuleTracers,
            // ModuleTrajectories,
            // ModuleTrueSight,
            // ModuleVoidESP,
            // ModuleXRay,
            // codex end
            ModuleDebug,
            // codex start
            // ModuleZoom,
            // ModuleItemChams,
            // ModuleCrystalView,
            // ModuleSkinChanger,
            // ModuleProtectionZones,
            // ModuleCrosshair,
            // ModuleWings,
            // ModulePotionFX,
            // ModuleTotemEffect,
            // codex end

            // World
            // codex start
            // AutoMobHeal,
            // ModuleAirPlace,
            // ModuleAutoBuild,
            // ModuleAutoDisable,
            // ModuleAutoFarm,
            // ModuleAutoTool,
            // codex end
            // codex start
            // ModuleCrystalAura,
            // codex end
            // codex start
            // ModuleFastBreak,
            // ModuleFastPlace,
            // ModuleFucker,
            // ModuleAutoTrap,
            // ModuleBlockTrap,
            // ModuleNoSlowBreak,
            // ModuleLiquidFiller,
            // ModuleLiquidPlace,
            // ModuleProjectilePuncher,
            // codex end
            // codex start
            // ModuleScaffold,
            // codex end
            // codex start
            // ModuleTimer,
            // ModuleNuker,
            // ModuleExtinguish,
            // ModuleBedDefender,
            // ModuleBlockIn,
            // ModuleSurround,
            // ModulePacketMine,
            // ModuleHoleFiller,
            // ModuleStrongholdFinder,
            // ModuleNoInterpolation,
            // codex end
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
