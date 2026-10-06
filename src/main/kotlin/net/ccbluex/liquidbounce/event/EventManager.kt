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
@file:Suppress("MaxLineLength") // Original-line markers preserve upstream snippets verbatim.
//codex end

package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.Object2ReferenceRBTreeMap
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import net.ccbluex.liquidbounce.annotations.Tag
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.module.KillAuraModulePolicyTodoAi
import net.ccbluex.liquidbounce.features.misc.SelfDestruct.isDestructed
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException

/**
 * Contains all classes of events. Used to create lookup tables ahead of time
 */
@JvmField
// codex start
internal val ALL_EVENT_CLASSES = emptyArray<Class<out Event>>()
//codex end

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
            // codex start
            if (!eventHook.handlerClass.running ||
                !KillAuraModulePolicyTodoAi.allowsListener(eventHook.handlerClass)) { //codex (if (!eventHook.handlerClass.running) {)
                //codex end
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
