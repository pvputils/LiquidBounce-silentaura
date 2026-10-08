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
package net.ccbluex.liquidbounce.features.misc

// codex start
//
// private val modMenuPresent = runCatching {
//     Class.forName("com.terraformersmc.modmenu.ModMenu")
//     true
// }.getOrDefault(false)
//
// codex end
object SelfDestruct {
    var isDestructed = false
    // codex start
    //
    // private val clientModIds: Set<String>
    //     get() = buildSet {
    //         add("liquidbounce")
    //         AddonManager.addons.forEach { add(it.id) }
    //     }
    //
    // /**
    //  * Attempt to destruct the client
    //  */
    // fun destructClient() {
    //     isDestructed = true
    //     mc.schedule(::restoreVanilla)
    //
    //     if (modMenuPresent) {
    //         for (id in clientModIds) {
    //             ModMenuCompatibility.INSTANCE.removeModUnchecked(id)
    //         }
    //     }
    //
    //     mc.gui.hud.chat.recentChat.removeIf {
    //         it.startsWith(CommandManager.GlobalSettings.prefix)
    //     }
    //
    //     // Cancel all async tasks
    //     ioScope.cancel()
    //
    //     callEvent(ClientShutdownEvent)
    //     EventManager.unregisterAll()
    //
    //     // Disable all modules
    //     // Be careful to not trigger ConfigManager saving, but this should be prevented by [isDestructed]
    //     // and unregistering all events
    //     for (module in ModuleManager) {
    //         module.enabled = false
    //     }
    //     ModuleManager.clear()
    // }
    //
    // private fun restoreVanilla() {
    //     ScreenManager.restoreOriginalScreen()
    //     mc.updateTitle()
    //     mc.window.setIcon(
    //         mc.vanillaPackResources.fullResources(),
    //         if (SharedConstants.getCurrentVersion().stable()) IconSet.RELEASE else IconSet.SNAPSHOT
    //     )
    // }
    //
    // fun wipeClient() = thread(name = "wipe-client") {
    //     // Wait for the client to be destructed
    //     sleep(1000L)
    //
    //     // Clear log folder
    //     mc.gameDirectory.resolve("logs").listFiles()?.forEach {
    //         runCatching {
    //             it.delete()
    //         }
    //     }
    //
    //     // Delete LiquidBounce folder and its content
    //     runCatching {
    //         ConfigSystem.rootFolder.deleteRecursively()
    //     }
    //
    //     val idsToWipe = clientModIds
    //
    //     FabricLoaderImpl.INSTANCE.allMods.filter {
    //         it.metadata.id in idsToWipe
    //     }.forEach { mod ->
    //         // Delete JAR file
    //         runCatching {
    //             val origin = mod.origin
    //
    //             for (path in origin.paths) {
    //                 runCatching {
    //                     path.deleteIfExists()
    //                 }
    //             }
    //         }
    //
    //         // Remove from Fabric Loader Impl
    //         runCatching {
    //             FabricLoaderImpl.INSTANCE.modsInternal.remove(mod)
    //         }
    //     }
    //
    //     // Jars staged this session belong to no mod container yet.
    //     AddonInstaller.wipeManagedJars()
    //
    //     // History clear
    //     mc.gui.hud.chat.clearMessages(true)
    // }
    //
    // codex end
}
