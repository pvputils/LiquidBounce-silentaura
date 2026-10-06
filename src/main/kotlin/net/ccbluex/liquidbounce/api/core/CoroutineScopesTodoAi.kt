package net.ccbluex.liquidbounce.api.core

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.minecraft.ReportedException

val ioScope = CoroutineScope(
    Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        if (throwable is ReportedException) ErrorHandler.fatal(throwable, additionalMessage = "IO scope")
    }
)
