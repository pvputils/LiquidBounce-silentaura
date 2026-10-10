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

package net.ccbluex.liquidbounce.event.events

import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.event.Event

// codex start
// @Tag("themeColorChange")
// class ThemeColorChangeEvent(val themeId: String, val name: String, val value: Color4b) : Event(), WebSocketEvent
//
// @Deprecated(
//     "The `clickGuiScaleChange` event has been deprecated.",
//     ReplaceWith("ClickGuiScaleChangeEvent"),
//     DeprecationLevel.WARNING
// )
// codex end
// codex start
// @Tag("clickGuiScaleChange")
// class ClickGuiScaleChangeEvent(val value: Float) : Event(), WebSocketEvent
// // codex start
// //
// // @Tag("clickGuiValueChange")
// // class ClickGuiValueChangeEvent(val configurable: ValueGroup) : Event(), WebSocketEvent {
// //     override val serializeAsync get() = false
// // }
// //
// // @Tag("spaceSeperatedNamesChange")
// // class SpaceSeperatedNamesChangeEvent(val value: Boolean) : Event(), WebSocketEvent
// // codex end
// codex end

@Tag("clientStart")
object ClientStartEvent : Event()

@Tag("clientShutdown")
object ClientShutdownEvent : Event()
// codex start
//
// @Tag("clientLanguageChanged")
// class ClientLanguageChangedEvent : Event(), WebSocketEvent
// codex end
// codex start
//
// @Tag("valueChanged")
// class ValueChangedEvent(val value: Value<*>) : Event(), WebSocketEvent
// // codex start
// //
// // @Tag("moduleActivation")
// // class ModuleActivationEvent(val moduleName: String) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @AddonApi
// // @Tag("moduleToggle")
// // class ModuleToggleEvent(val moduleName: String, val hidden: Boolean, val enabled: Boolean) : Event(),
// WebSocketEvent
// // // codex start
// // //
// // // @AddonApi
// // // @Tag("refreshArrayList")
// // // object RefreshArrayListEvent : Event(), WebSocketEvent
// // // codex end
// // // codex start
// // //
// // // @AddonApi
// // // @Tag("friendChange")
// // // class FriendChangeEvent(val name: String, val added: Boolean) : Event()
// // // codex end
// // codex end
// codex end
// codex start
//
// @AddonApi
// @Tag("notification")
// class NotificationEvent(val title: String, val message: String, val severity: Severity) : Event(), WebSocketEvent {
//     @AddonApi
//     enum class Severity {
//         INFO, SUCCESS, ERROR, ENABLED, DISABLED
//     }
// }
// // codex start
// //
// // @Tag("gameModeChange")
// // class GameModeChangeEvent(val gameMode: GameType) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("targetChange")
// // class TargetChangeEvent(val target: PlayerData?) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("blockCountChange")
// // class BlockCountChangeEvent(val nextBlock: Block?, val count: Int?) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("bedStateChange")
// // class BedStateChangeEvent(val bedStates: Collection<BedState>) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("clientChatStateChange")
// // class ClientChatStateChange(val state: State) : Event(), WebSocketEvent {
// //     enum class State {
// //         @SerializedName("connecting")
// //         CONNECTING,
// //
// //         @SerializedName("connected")
// //         CONNECTED,
// //
// //         @SerializedName("logon")
// //         LOGGING_IN,
// //
// //         @SerializedName("loggedIn")
// //         LOGGED_IN,
// //
// //         @SerializedName("disconnected")
// //         DISCONNECTED,
// //
// //         @SerializedName("authenticationFailed")
// //         AUTHENTICATION_FAILED,
// //     }
// // }
// // codex end
// // codex start
// //
// // @Tag("clientChatMessage")
// // class ClientChatMessageEvent(
// //     val user: AxoUser,
// //     val message: String,
// //     val chatGroup: ChatGroup,
// // ) : Event(), WebSocketEvent {
// //     enum class ChatGroup(override val tag: String) : Tagged {
// //         @SerializedName("public")
// //         PUBLIC_CHAT("PublicChat"),
// //
// //         @SerializedName("private")
// //         PRIVATE_CHAT("PrivateChat"),
// //     }
// // }
// // codex end
// // codex start
// //
// // @Tag("clientChatError")
// // class ClientChatErrorEvent(val error: String) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("clientChatJwtToken")
// // // Do not define as WebSocket event, because it contains sensitive data
// // class ClientChatJwtTokenEvent(val jwt: String) : Event()
// // codex end
// // codex start
// //
// // @Tag("accountManagerMessage")
// // class AccountManagerMessageEvent(val message: String) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("accountManagerLogin")
// // class AccountManagerLoginResultEvent(val username: String? = null, val error: String? = null) : Event(),
// // WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("accountManagerAddition")
// // class AccountManagerAdditionResultEvent(
// //     val username: String? = null, val error: String? = null
// // ) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("accountManagerRemoval")
// // class AccountManagerRemovalResultEvent(val username: String?) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("proxyCheckResult")
// // class ProxyCheckResultEvent(val proxy: Proxy? = null, val error: String? = null) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("browserReady")
// // object BrowserReadyEvent : Event()
// //
// // // codex start
// // // @Tag("virtualScreen")
// // // class VirtualScreenEvent(
// // //     val type: CustomScreenType,
// // //     @Deprecated("Use `type` instead") val screenName: String = type.routeName,
// // //     val action: Action
// // // ) : Event(), WebSocketEvent {
// // //
// // //     enum class Action {
// // //         @SerializedName("open")
// // //         OPEN,
// // //
// // //         @SerializedName("close")
// // //         CLOSE
// // //     }
// // //
// // // }
// // //
// // // codex end
// // codex end
// // codex start
// // @Tag("serverPinged")
// // class ServerPingedEvent(val server: ServerData) : Event(), WebSocketEvent
// // // codex start
// // //
// // // @Tag("componentsUpdate")
// // // class ComponentsUpdateEvent(
// // //     val source: Source,
// // //     val components: List<HudComponent>,
// // //     val themeId: String? = null,
// // // ) : Event(), WebSocketEvent {
// // //     enum class Source {
// // //         @SerializedName("native")
// // //         NATIVE,
// // //
// // //         @SerializedName("theme")
// // //         THEME,
// // //     }
// // //
// // //     override val serializer get() = accessibleInteropGson
// // //
// // //     override val serializeAsync get() = false
// // // }
// // // codex end
// // codex end
// codex end

@Tag("rotationUpdate")
object RotationUpdateEvent : Event()
// codex start
//
// @Tag("resourceReload")
// object ResourceReloadEvent : Event()
// // codex start
// //
// // @Tag("scaleFactorChange")
// // class ScaleFactorChangeEvent(val scaleFactor: Int) : Event(), WebSocketEvent
// // // codex start
// // //
// // // @Tag("scheduleInventoryAction")
// // // class ScheduleInventoryActionEvent(val schedule: MutableList<InventoryAction.Chain> = mutableListOf()) :
// Event() {
// // //
// // //     fun schedule(
// // //         constrains: InventoryConstraints,
// // //         action: InventoryAction,
// // //         priority: Priority = Priority.NORMAL
// // //     ) {
// // //         this.schedule.add(InventoryAction.Chain(constrains, listOf(action), priority))
// // //     }
// // //
// // //     fun schedule(
// // //         constrains: InventoryConstraints,
// // //         vararg actions: InventoryAction,
// // //         priority: Priority = Priority.NORMAL
// // //     ) {
// // //         this.schedule.add(InventoryAction.Chain(constrains, actions.unmodifiable(), priority))
// // //     }
// // //
// // //     fun schedule(
// // //         constrains: InventoryConstraints,
// // //         actions: List<InventoryAction>,
// // //         priority: Priority = Priority.NORMAL
// // //     ) {
// // //         this.schedule.add(InventoryAction.Chain(constrains, actions, priority))
// // //     }
// // // }
// // // codex end
// // codex end
// codex end
// codex start
//
// @Tag("selectHotbarSlotSilently")
// class SelectHotbarSlotSilentlyEvent(val requester: Any?, val slot: Int): CancellableEvent()
// // codex start
// //
// // @Tag("browserUrlChange")
// // class BrowserUrlChangeEvent(val index: Int, val url: String) : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("userLoggedIn")
// // object UserLoggedInEvent : Event(), WebSocketEvent
// // codex end
// // codex start
// //
// // @Tag("userLoggedOut")
// // object UserLoggedOutEvent : Event(), WebSocketEvent
// // codex end
// codex end
