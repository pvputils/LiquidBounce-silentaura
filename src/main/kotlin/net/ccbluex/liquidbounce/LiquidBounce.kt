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
package net.ccbluex.liquidbounce

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import kotlinx.coroutines.internal.isMissing
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.global.GlobalManager
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.HAS_AMD_VEGA_APU
import net.ccbluex.liquidbounce.utils.aiming.PostRotationExecutor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.GitInfo
import net.ccbluex.liquidbounce.utils.client.InteractionTracker
import net.ccbluex.liquidbounce.utils.client.clientIdentifier
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.input.InputTracker
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.io.readText
import net.ccbluex.liquidbounce.utils.network.LocalPlayerFallDamageTracker
import net.ccbluex.liquidbounce.utils.network.PositionPacketSeparator
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.time.measureTime

/**
 * LiquidBounce
 *
 * A free mixin-based injection hacked-client for Minecraft using FabricMC.
 *
 * @author kawaiinekololis (@team CCBlueX)
 */
object LiquidBounce : EventListener {

    /**
     * CLIENT INFORMATION
     *
     * WARNING: Please read the GNU General Public License
     */
    const val CLIENT_NAME = "LiquidBounce"
    const val CLIENT_AUTHOR = "CCBlueX"

    private object Client : Config("Client") {
        val version = text("Version", GitInfo.version())
            .immutable()
        val commit = text("Commit", GitInfo.get("git.commit.id.abbrev")?.let { "git-$it" } ?: "unknown")
            .immutable()
        val branch = text("Branch", GitInfo.branch())
            .immutable()

        init {
            ConfigSystem.root(this)

            version.onChange { previousVersion ->
                runCatching {
                    ConfigSystem.backup("automatic_${previousVersion}-${version.inner}")
                }.onFailure {
                    logger.error("Unable to create backup", it)
                }

                previousVersion
            }
        }
    }

    val clientVersion by Client.version
    val clientCommit by Client.commit
    val clientBranch by Client.branch

    /**
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of semantic versioning.
     *
     * TODO: Replace this approach with full semantic versioning.
     */
    const val IN_DEVELOPMENT = true

    /**
     * Client logger to print out console messages
     */
    val logger get() = net.ccbluex.liquidbounce.utils.client.logger

    var taskManager: TaskManager? = null

    var isInitialized = false
        private set

    /**
     * Creates an [net.minecraft.resources.Identifier] starts with [CLIENT_NAME].
     *
     * Warning: Use [clientIdentifier] to prevent silent `<clinit>` invocation
     */
    @JvmStatic
    fun identifier(path: String): Identifier = clientIdentifier(path)

    /**
     * Gets client resource.
     *
     * @param path prefix `/resources/liquidbounce/`
     * @throws IllegalArgumentException if the resource is not found
     */
    @JvmStatic
    fun resource(path: String): InputStream =
        LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/$path")
            ?: throw IllegalArgumentException("Resource $path not found")

    /**
     * Gets client resource as string.
     *
     * @param path prefix `/resources/liquidbounce/`
     * @throws IllegalArgumentException if the resource is not found
     */
    @JvmStatic
    fun resourceToString(path: String): String =
        resource(path).use { it.readText() }

    /**
     * Initializes the client, called when
     * we reached the last stage of the splash screen.
     *
     * The thread should be the main render thread.
     */
    @OptIn(InternalCoroutinesApi::class)
    private fun initializeClient(
        workerDispatcher: CoroutineDispatcher,
        renderThreadDispatcher: CoroutineDispatcher,
    ): CompletableFuture<Void?> = CoroutineScope(
        renderThreadDispatcher + CoroutineName("$CLIENT_NAME Initializer")
    ).future<Void?> {
        if (isInitialized) {
            return@future null
        }

        // Ensure we are on the render thread
        RenderSystem.assertOnRenderThread()
        check(!Dispatchers.Main.isMissing())

        // Initialize managers and features
        Client
        initializeManagers(renderThreadDispatcher)
        initializeFeatures()
        initializeResources(workerDispatcher)
        prepareGuiStage(renderThreadDispatcher)

        // Register shutdown hook in case [ClientShutdownEvent] is not called
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownClient))

        // Check for AMD Vega iGPU
        if (HAS_AMD_VEGA_APU) {
            logger.info("AMD Vega iGPU detected, enabling different line smooth handling. " +
                "If you believe this is a mistake, please create an issue at " +
                "https://github.com/CCBlueX/LiquidBounce/issues.")
        }

        // Do backup before loading configs
        if (!ConfigSystem.isFirstLaunch && !Client.jsonFile.exists()) {
            runCatching {
                ConfigSystem.backup("automatic_${Client.version.inner}")
            }.onFailure {
                logger.error("Unable to create backup", it)
            }
        }

        // Load all configurations
        ConfigSystem.loadAll()
        // codex start
        // AddonManager.notifyStarted()
        // codex end

        isInitialized = true
        logger.info("$CLIENT_NAME has been successfully initialized.")
        null
    }.exceptionally { throwable ->
        ErrorHandler.fatal(throwable, additionalMessage = "$CLIENT_NAME initializer")
    }

    /**
     * Initializes managers for Event Listener registration.
     */
    private suspend fun initializeManagers(
        renderThreadDispatcher: CoroutineDispatcher,
    ) = withContext(renderThreadDispatcher) {
        // Config
        ConfigSystem

        // Utility
        RenderedEntities
        ChunkScanner
        InputTracker

        // Feature managers
        ModuleManager
        // codex start
        // CommandManager
        // ProxyManager
        // AccountManager
        // codex end

        // Utility managers
        RotationManager
        BlinkManager
        LocalPlayerFallDamageTracker
        PositionPacketSeparator
        InteractionTracker
        CombatManager
        FriendManager
        InventoryManager
        // codex start
        // EnderChestInventoryTracker
        // ActiveServerList
        // ConfigSystem.root(ClientAccountManager)
        // ConfigSystem.root(SpooferManager)
        // codex end
        ConfigSystem.root(GlobalManager)
        // codex start
        // ConfigSystem.root(MarketplaceManager)
        // ConfigSystem.root(ConfigTracker)
        // codex end
        PostRotationExecutor
        // codex start
        // ServerObserver
        // ItemImageAtlas
        //
        // AddonManager.discover()
        // codex end
    }

    /**
     * Initializes in-built and add-on features.
     */
    private fun initializeFeatures() {
        // Register commands and modules
        // codex start
        // CommandManager.registerInbuilt()
        // codex end
        ModuleManager.registerInbuilt()
        // codex start
        //
        // AddonManager.registerCategories()
        // AddonManager.initializeAddons()
        // codex end
    }

    /**
     * Simultaneously initializes resources
     * such as translations, cosmetics, player heads, configs and so on,
     * which do not rely on the main thread.
     */
    private suspend fun initializeResources(
        dispatcher: CoroutineDispatcher,
    ) = withContext(dispatcher) {
        // codex start
        // logger.info("Initializing API...")
        // // Lookup API config
        // ApiConfig.config
        //
        // codex end
        supervisorScope {
            launch {
                // Load translations
                LanguageManager.loadDefault()
            }
            // codex start
            // launch {
            //     val update = withTimeoutOrNull(8.seconds) { ClientUpdate.update.await() } ?: return@launch
            //     logger.info("[Update] Update available: $clientVersion -> ${update.lbVersion}")
            // }
            // launch {
            //     // Load cosmetics
            //     CosmeticService.refreshCarriers(force = true) {
            //         logger.info("Successfully loaded ${CosmeticService.carriers.size} cosmetics carriers.")
            //     }
            // }
            // launch {
            //     // Download player heads
            //     HeadsCreativeModeTab.heads.getFinalState()
            // }
            // launch {
            //     MarketplaceConfigs.refresh()
            // }
            // launch {
            //     MarketplaceItems.refresh()
            // }
            // launch {
            //     MarketplaceManager.fillAuthors()
            // }
            // launch {
            //     IpInfoApi.original
            // }
            // launch {
            //     ConfigSystem.load(ClientAccountManager)
            //     if (ClientAccount.ENV_ACCOUNT != null) {
            //         ClientAccountManager.clientAccount = ClientAccount.ENV_ACCOUNT
            //     }
            //
            //     if (ClientAccountManager.clientAccount != ClientAccount.EMPTY_ACCOUNT) {
            //         runCatching {
            //             ClientAccountManager.clientAccount.renew()
            //         }.onFailure {
            //             logger.error("Failed to renew client account token.", it)
            //             if (it.httpException?.isInvalidGrant == true) {
            //                 ClientAccountManager.clientAccount = ClientAccount.EMPTY_ACCOUNT
            //                 ConfigSystem.store(ClientAccountManager)
            //             }
            //         }.onSuccess {
            //             logger.info("Successfully renewed client account token.")
            //         }
            //     }
            // }
            // codex end
        }
        // codex start
        //
        // logger.info("API initialization done.")
        // codex end
    }

    /**
     * Prepares the GUI stage of the client.
     * This will load [ThemeManager], as well as the [BrowserBackendManager] and [ClientInteropServer].
     */
    private suspend fun prepareGuiStage(
        dispatcher: CoroutineDispatcher
    ) = withContext(dispatcher) {
        RenderSystem.assertOnRenderThread()

        // codex start
        // BrowserBackendManager.init()
        // ClientInteropServer.start()
        //
        // // Preload marketplace items
        // ConfigSystem.load(MarketplaceManager)
        // MarketplaceManager.subscribedItems.forEach(SubscribedItem::restoreRetired)
        // AddonInstaller.stageSubscribedAddons()
        // MarketplaceManager.reloadHandlers()
        //
        // if (!ClientInteropServer.isSkipping) {
        //     ThemeManager.init()
        //     ConfigSystem.load(ThemeManager)
        //     ThemeManager.load()
        // }
        //
        // BlurEffectRenderer
        // ScreenManager
        //
        // // Holds the chosen browser backend
        // ConfigSystem.load(GlobalManager)
        //
        // codex end
        taskManager = TaskManager(ioScope).apply {
            // codex start
            // // Either immediately starts browser or spawns a task to request browser dependencies,
            // // and then starts the browser through render thread.
            // BrowserBackendManager.makeDependenciesAvailable(this)
            // codex end

            // Initialize deep learning engine as task, because we cannot know if DJL will request
            // resources from the internet.
            launch("Deep Learning") { task ->
                runCatching {
                    DeepLearningEngine.init(task)
                    ModelManager.load()
                    DeepLearningEngine.markInitialized()
                }.onFailure { exception ->
                    task.subTasks.clear()
                    DeepLearningEngine.markUnavailable()

                    // LiquidBounce can still run without deep learning,
                    // and we don't want to crash the client if it fails.
                    logger.info("Failed to initialize deep learning.", exception)
                }
            }

            // codex start
            // launch("Marketplace") { task ->
            //     runCatching {
            //         MarketplaceManager.updateAll(task)
            //     }.onFailure { exception ->
            //         logger.error("Failed to update marketplace items.", exception)
            //     }
            //
            //     task.isCompleted = true
            // }
            // codex end
        }

        // Prepare glyph manager
        val duration = measureTime {
            FontManager.createGlyphManager()
        }
        logger.info("Completed loading fonts in ${duration.inWholeMilliseconds} ms.")
        logger.info("Fonts: [ ${FontManager.fontFaces.keys.joinToString()} ]")
    }

    /**
     * Shuts down the client. This will save all configurations and stop all running tasks.
     */
    private fun shutdownClient() {
        if (!isInitialized) {
            return
        }
        isInitialized = false
        logger.info("Shutting down client...")

        // Unregister all event listener and stop all running tasks
        ChunkScanner.stopThread()
        FontManager.closeGlyphManager()
        EventManager.unregisterAll()

        // codex start
        // // Shutdown HTTP server
        // ioScope.launch {
        //     ClientInteropServer.stop()
        // }
        //
        // AddonManager.notifyStopping()
        //
        // codex end
        // Save all configurations
        ConfigSystem.storeAll()

        // Shutdown browser
        // codex start
        // BrowserBackendManager.stop()
        // codex end
    }

    /**
     * Should be executed to start the client.
     */
    @Suppress("unused")
    private val startHandler = handler<ClientStartEvent> {
        runCatching {
            logger.info("Launching $CLIENT_NAME v$clientVersion by $CLIENT_AUTHOR")
            // Print client information
            logger.info("Client Version: $clientVersion ($clientCommit)")
            logger.info("Client Branch: $clientBranch")
            logger.info("Operating System: ${System.getProperty("os.name")} (${System.getProperty("os.version")})")
            logger.info("Java Version: ${System.getProperty("java.version")}")
            logger.info("Screen Resolution: ${mc.window.screenWidth}x${mc.window.screenHeight}")
            logger.info("Refresh Rate: ${mc.window.activeVideoMode?.refreshRate} Hz")

            // Initialize event manager
            EventManager

            // Register resource reloader
            val resourceManager = mc.resourceManager
            if (resourceManager is ReloadableResourceManager) {
                resourceManager.registerReloadListener(ClientResourceReloader)
                // codex start
                // resourceManager.registerReloadListener(ThemeManager.reloader)
                // codex end
            } else {
                logger.warn("Failed to register resource reloader!")

                // Run resource reloader directly as fallback
                initializeClient(
                    workerDispatcher = Dispatchers.Default,
                    renderThreadDispatcher = Dispatchers.Main,
                // codex start
                // ).thenCompose {
                //     ThemeManager.reloader.reload()
                // }
                // codex end
                )
            }
        }.onFailure {
            ErrorHandler.fatal(it, additionalMessage = "Client start")
    // codex start
    //     }
    // }
    //
    // @Suppress("unused")
    // private val screenHandler = handler<ScreenEvent>(priority = FIRST_PRIORITY) { event ->
    //     val taskManager = taskManager ?: return@handler
    //
    //     val selection = BrowserBackendManager.pendingSelection
    //     if (selection != null && !selection.isCompleted) {
    //         if (event.screen !is BrowserSelectionScreen) {
    //             event.cancelEvent()
    //             mc.gui.setScreen(BrowserSelectionScreen(BrowserBackendManager.selectableBackends, selection))
    //         }
    //         return@handler
    //     }
    //
    //     if (!taskManager.isCompleted && event.screen !is TaskProgressScreen) {
    //         event.cancelEvent()
    //         mc.gui.setScreen(TaskProgressScreen("Loading Required Libraries", taskManager))
    // codex end
        }
    }

    /**
     * Resource reloader which is executed on client start and reload.
     * This is used to run async tasks without blocking the main thread.
     *
     * For now this is only used to check for updates and request additional information from the internet.
     *
     * @see net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener
     * @see PreparableReloadListener
     */
    private object ClientResourceReloader : PreparableReloadListener {
        override fun reload(
            store: PreparableReloadListener.SharedState,
            prepareExecutor: Executor,
            synchronizer: PreparableReloadListener.PreparationBarrier,
            applyExecutor: Executor
        ): CompletableFuture<Void> {
            return synchronizer.wait(net.minecraft.util.Unit.INSTANCE)
                .thenCompose {
                    val prepareDispatcher = prepareExecutor.asCoroutineDispatcher()
                    val applyDispatcher = applyExecutor.asCoroutineDispatcher()
                    initializeClient(
                        workerDispatcher = prepareDispatcher,
                        renderThreadDispatcher = applyDispatcher,
                    )
                }
        }

        override fun getName() = CLIENT_NAME
    }

    /**
     * Should be executed to stop the client.
     */
    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        shutdownClient()
    }

}
