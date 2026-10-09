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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.Object2ReferenceRBTreeMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PerspectiveEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerUseMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.misc.SelfDestruct.isDestructed
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
@JvmField
internal val ALL_EVENT_CLASSES: Array<Class<out Event>> = arrayOf(
    GameTickEvent::class.java,
    // codex start
    // GameRenderTaskQueueEvent::class.java,
    // codex end
    // codex start
    // TickPacketProcessEvent::class.java,
    // codex end
    // codex start
    // BlockChangeEvent::class.java,
    // codex end
    // codex start
    // ChunkLoadEvent::class.java,
    // codex end
    // codex start
    // ChunkDeltaUpdateEvent::class.java,
    // codex end
    // codex start
    // ChunkUnloadEvent::class.java,
    // codex end
    DisconnectEvent::class.java,
    GameRenderEvent::class.java,
    // codex start
    // WorldFeatureSubmitEvent::class.java,
    // codex end
    WorldRenderEvent::class.java,
    OverlayRenderEvent::class.java,
    // codex start
    // ScreenRenderEvent::class.java,
    // WindowResizeEvent::class.java,
    // FramebufferResizeEvent::class.java,
    // codex end
    // codex start
    // WindowTitleEvent::class.java,
    // codex end
    MouseButtonEvent::class.java,
    // codex start
    // MouseScrollEvent::class.java,
    // codex end
    // codex start
    // MouseCursorEvent::class.java,
    // codex end
    KeyboardKeyEvent::class.java,
    // codex start
    // KeyboardCharEvent::class.java,
    // codex end
    // codex start
    // InputHandleEvent::class.java,
    // codex end
    MovementInputEvent::class.java,
    SprintEvent::class.java,
    // codex start
    // KeyEvent::class.java,
    // codex end
    MouseRotationEvent::class.java,
    // codex start
    // KeybindChangeEvent::class.java,
    // codex end
    // codex start
    // KeybindIsPressedEvent::class.java,
    // codex end
    AttackEntityEvent::class.java,
    // codex start
    // SessionEvent::class.java,
    // codex end
    ScreenEvent::class.java,
    // codex start
    // ChatSendEvent::class.java,
    // ChatReceiveEvent::class.java,
    // codex end
    // codex start
    // UseCooldownEvent::class.java,
    // codex end
    // codex start
    // BlockShapeEvent::class.java,
    // codex end
    // codex start
    // BlockBreakingProgressEvent::class.java,
    // codex end
    // codex start
    // BlockVelocityMultiplierEvent::class.java,
    // BlockSlipperinessMultiplierEvent::class.java,
    // codex end
    // codex start
    // EntityMarginEvent::class.java,
    // codex end
    // codex start
    // EntityHealthUpdateEvent::class.java,
    // codex end
    // codex start
    // HealthUpdateEvent::class.java,
    // codex end
    // codex start
    // DeathEvent::class.java,
    // codex end
    // codex start
    // PlayerTickEvent::class.java,
    // codex end
    // codex start
    // PlayerPostTickEvent::class.java,
    // codex end
    // codex start
    // PlayerMovementTickEvent::class.java,
    // codex end
    // codex start
    // PlayerNetworkMovementTickEvent::class.java,
    // codex end
    // codex start
    // PlayerPushOutEvent::class.java,
    // codex end
    PlayerMoveEvent::class.java,
    // codex start
    // PlayerJumpEvent::class.java,
    // codex end
    // codex start
    // PlayerAfterJumpEvent::class.java,
    // codex end
    PlayerUseMultiplier::class.java,
    // codex start
    // PlayerInteractItemEvent::class.java,
    // codex end
    // codex start
    // PlayerInteractedItemEvent::class.java,
    // codex end
    // codex start
    // ClientPlayerInventoryEvent::class.java,
    // codex end
    PlayerVelocityStrafe::class.java,
    // codex start
    // PlayerStrideEvent::class.java,
    // codex end
    PlayerSafeWalkEvent::class.java,
    // codex start
    // CancelBlockBreakingEvent::class.java,
    // codex end
    // codex start
    // PlayerStepEvent::class.java,
    // codex end
    // codex start
    // PlayerStepSuccessEvent::class.java,
    // codex end
    // codex start
    // FluidPushEvent::class.java,
    // codex end
    // codex start
    // PipelineEvent::class.java,
    // codex end
    PacketEvent::class.java,
    ClientStartEvent::class.java,
    ClientShutdownEvent::class.java,
    // codex start
    // ClientLanguageChangedEvent::class.java,
    // codex end
    // codex start
    // ValueChangedEvent::class.java,
    // codex end
    // codex start
    // ModuleActivationEvent::class.java,
    // codex end
    // codex start
    // ModuleToggleEvent::class.java,
    // codex end
    // codex start
    // FriendChangeEvent::class.java,
    // codex end
    // codex start
    // NotificationEvent::class.java,
    // codex end
    // codex start
    // ClientChatStateChange::class.java,
    // ClientChatMessageEvent::class.java,
    // ClientChatErrorEvent::class.java,
    // ClientChatJwtTokenEvent::class.java,
    // codex end
    WorldChangeEvent::class.java,
    // codex start
    // AccountManagerMessageEvent::class.java,
    // AccountManagerAdditionResultEvent::class.java,
    // AccountManagerRemovalResultEvent::class.java,
    // AccountManagerLoginResultEvent::class.java,
    // VirtualScreenEvent::class.java,
    // codex end
    // codex start
    // FpsChangeEvent::class.java,
    // codex end
    // codex start
    // FpsLimitEvent::class.java,
    // ClientPlayerDataEvent::class.java,
    // ClientPlayerEffectEvent::class.java,
    // codex end
    RotationUpdateEvent::class.java,
    // codex start
    // RefreshArrayListEvent::class.java,
    // codex end
    // codex start
    // BrowserReadyEvent::class.java,
    // ServerConnectEvent::class.java,
    // ServerPingedEvent::class.java,
    // TargetChangeEvent::class.java,
    // BlockCountChangeEvent::class.java,
    // BedStateChangeEvent::class.java,
    // codex end
    // codex start
    // GameModeChangeEvent::class.java,
    // codex end
    // codex start
    // ComponentsUpdateEvent::class.java,
    // codex end
    // codex start
    // ResourceReloadEvent::class.java,
    // codex end
    // codex start
    // ProxyCheckResultEvent::class.java,
    // ScaleFactorChangeEvent::class.java,
    // codex end
    // codex start
    // DrawOutlinesEvent::class.java,
    // codex end
    // codex start
    // OverlayMessageEvent::class.java,
    // codex end
    // codex start
    // ScheduleInventoryActionEvent::class.java,
    // codex end
    // codex start
    // SelectHotbarSlotSilentlyEvent::class.java,
    // codex end
    // codex start
    // SpaceSeperatedNamesChangeEvent::class.java,
    // ClickGuiScaleChangeEvent::class.java,
    // ThemeColorChangeEvent::class.java,
    // BrowserUrlChangeEvent::class.java,
    // codex end
    TagEntityEvent::class.java,
    // codex start
    // MouseScrollInHotbarEvent::class.java,
    // codex end
    // codex start
    // PlayerFluidCollisionCheckEvent::class.java,
    // codex end
    // codex start
    // PlayerContainerInputEvent::class.java,
    // codex end
    PlayerSneakMultiplier::class.java,
    PerspectiveEvent::class.java,
    // codex start
    // ItemLoreQueryEvent::class.java,
    // codex end
    // codex start
    // EntityEquipmentChangeEvent::class.java,
    // codex end
    // codex start
    // ClickGuiValueChangeEvent::class.java,
    // codex end
    // codex start
    // BlockAttackEvent::class.java,
    // codex end
    // codex start
    // BlinkPacketEvent::class.java,
    // codex end
    // codex start
    // AllowAutoJumpEvent::class.java,
    // codex end
    // codex start
    // WorldEntityRemoveEvent::class.java,
    // codex end
    // codex start
    // TitleEvent.Title::class.java,
    // TitleEvent.Subtitle::class.java,
    // TitleEvent.Fade::class.java,
    // TitleEvent.Clear::class.java,
    // codex end
    // codex start
    // ClosedCaptionsEvent::class.java,
    // UserLoggedInEvent::class.java,
    // UserLoggedOutEvent::class.java,
    // codex end
)

inline fun <reified E : Event> eventFlow(): SharedFlow<E> =
    EventManager.eventFlow(E::class.java)

/**
 * Swapped as one object, so readers never see the tables disagree.
 */
private class EventTables(@JvmField val classes: Set<Class<out Event>>, previous: EventTables?) {

    @JvmField
    val registry: Map<Class<out Event>, EventHookRegistry<in Event>> = classes.associateWithTo(
        Reference2ObjectOpenHashMap(classes.size)
    ) { previous?.registry?.get(it) ?: EventHookRegistry() }

    @JvmField
    val flows: Map<Class<out Event>, MutableSharedFlow<Event>> = classes.associateWithTo(
        Reference2ObjectOpenHashMap(classes.size)
    ) { previous?.flows?.get(it) ?: MutableSharedFlow(replay = 0, extraBufferCapacity = 0) }

    @JvmField
    val classToName: Map<Class<out Event>, String> =
        Reference2ObjectOpenHashMap<Class<out Event>, String>(classes.size).apply {
            classes.forEach { eventClass ->
                eventClass.getAnnotation(Tag::class.java)?.let { put(eventClass, it.name) }
            }
        }

    @JvmField
    val nameToClass: Map<String, Class<out Event>> =
        Object2ReferenceRBTreeMap<String, Class<out Event>>(String.CASE_INSENSITIVE_ORDER).apply {
            classToName.forEach { (eventClass, name) -> put(name, eventClass) }
        }

}

/**
 * A modern and fast event handler using lambda handlers
 */
@AddonApi
object EventManager {

    @Volatile
    private var tables = EventTables(ALL_EVENT_CLASSES.toCollection(LinkedHashSet()), previous = null)

    val knownEventClasses: Set<Class<out Event>>
        get() = tables.classes

    /**
     * Looks up by [Tag] name, ignoring case.
     */
    fun eventClassByName(name: String): Class<out Event>? = tables.nameToClass[name]

    internal fun eventNameOrNull(eventClass: Class<out Event>): String? = tables.classToName[eventClass]

    @Synchronized
    fun registerEventClass(eventClass: Class<out Event>): Boolean {
        val current = tables
        if (eventClass in current.classes) {
            return false
        }

        eventClass.getAnnotation(Tag::class.java)?.let { tag ->
            val owner = current.nameToClass[tag.name]
            require(owner == null) {
                "Event name '${tag.name}' is already taken by ${owner!!.name}, " +
                    "cannot register ${eventClass.name}"
            }
        }

        tables = EventTables(LinkedHashSet(current.classes).apply { add(eventClass) }, current)
        return true
    }

    private fun tablesContaining(eventClass: Class<out Event>): EventTables {
        val current = tables
        if (eventClass in current.classes) {
            return current
        }

        registerEventClass(eventClass)
        return tables
    }

    /**
     * Used by handler methods
     */
    fun <T : Event> registerEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>): EventHook<T> {
        val handlers = tablesContaining(eventClass).registry.getValue(eventClass)

        @Suppress("UNCHECKED_CAST")
        val hook = eventHook as EventHook<in Event>

        handlers.addIfAbsent(hook)

        return eventHook
    }

    /**
     * Unregisters a handler.
     */
    fun <T : Event> unregisterEventHook(eventClass: Class<out Event>, eventHook: EventHook<T>) {
        @Suppress("UNCHECKED_CAST")
        tables.registry[eventClass]?.remove(eventHook as EventHook<in Event>)
    }

    fun unregisterEventHandler(eventListener: EventListener) {
        tables.registry.values.forEach {
            it.remove(eventListener)
        }
    }

    fun unregisterAll() {
        tables.registry.values.forEach {
            it.clear()
        }
    }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun <T : Event> callEvent(event: T): T {
        if (isDestructed) {
            return event
        }

        val eventType = event.javaClass
        val snapshot = tables
        val target = snapshot.registry[eventType] ?: return event

        event.isCompleted = false
        for (eventHook in target.snapshot) {
            @Suppress("UNCHECKED_CAST")
            eventHook as EventHook<T>
            if (!eventHook.handlerClass.running) {
                continue
            }

            try {
                eventHook.handler.accept(event)
            } catch (e: ReportedException) {
                ErrorHandler.fatal(
                    error = e,
                    needToReport = true,
                    additionalMessage = "Event (${eventType.simpleName}) handler of ${eventHook.handlerClass}"
                )
            } catch (e: Throwable) {
                logger.error(
                    "Exception while executing event handler of {}, event={}",
                    eventHook.handlerClass.javaClass.simpleName,
                    event,
                    e,
                )
            }
        }
        event.isCompleted = true

        @Suppress("UNCHECKED_CAST")
        (snapshot.flows.getValue(eventType) as MutableSharedFlow<T>).tryEmit(event)

        return event
    }

    /**
     * Gets a [SharedFlow] for the given event class.
     * The flow receives the event instances after all [EventHook]s are executed.
     * So the [Event.isCompleted] will be true when the event is emitted.
     */
    fun <T : Event> eventFlow(eventClass: Class<T>): SharedFlow<T> {
        @Suppress("UNCHECKED_CAST")
        return tablesContaining(eventClass).flows.getValue(eventClass) as SharedFlow<T>
    }
}
