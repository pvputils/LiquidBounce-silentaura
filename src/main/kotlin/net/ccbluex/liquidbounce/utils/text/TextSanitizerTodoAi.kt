package net.ccbluex.liquidbounce.utils.text

import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/** Retains legacy formatting conversion without the unrelated name replacement module. */
fun Component.sanitizeForeignInput(): FormattedCharSequence = this.asFormattedCharSequence()
fun FormattedCharSequence.sanitizeForeignInput(): FormattedCharSequence = this
