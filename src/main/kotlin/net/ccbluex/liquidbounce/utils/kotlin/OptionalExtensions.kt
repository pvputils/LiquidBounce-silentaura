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

@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.kotlin

import java.util.Optional
// codex start
// import java.util.OptionalDouble
// import java.util.OptionalInt
// import java.util.OptionalLong
// codex end

inline fun <T : Any> optional() = Optional.empty<T>()

inline fun <T : Any> optional(value: T?) = Optional.ofNullable(value)

// codex start
// inline fun <T : Any> optional(block: () -> T?) = Optional.ofNullable(block())
//
// inline fun optional(value: Int): OptionalInt = OptionalInt.of(value)
//
// inline fun optional(value: Long): OptionalLong = OptionalLong.of(value)
//
// inline fun optional(value: Double): OptionalDouble = OptionalDouble.of(value)
// codex end

// codex start
// inline fun OptionalInt.toNullable(): Int? = if (isPresent) asInt else null
// codex end

// codex start
// inline fun OptionalLong.toNullable(): Long? = if (isPresent) asLong else null
// codex end

// codex start
// inline fun OptionalDouble.toNullable(): Double? = if (isPresent) asDouble else null
// codex end
