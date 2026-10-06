package net.ccbluex.liquidbounce.integration.screen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelManager
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.logger

/** AI rotation remains optional; native screens and normal combat never wait for its downloads. */
object KillAuraAiLoaderTodoAi {
    private var started = false
    fun start() {
        if (started) return
        started = true
        TaskManager(CoroutineScope(SupervisorJob() + Dispatchers.IO)).launch("KillAura AI") { task ->
            runCatching {
                DeepLearningEngine.init(task)
                ModelManager.load()
                DeepLearningEngine.markInitialized()
            }.onFailure {
                DeepLearningEngine.markUnavailable()
                logger.warn("KillAura AI unavailable; configured fallback rotations remain active.", it)
            }
            task.isCompleted = true
        }
    }
}
