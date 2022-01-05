/*
 * Copyright (C) 2022 Nikita Bobko
 *
 * This file is part of Todomail.
 *
 * Todomail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Todomail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Todomail. If not, see <https://www.gnu.org/licenses/>.
 */

package bobko.todomail.util

import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * Test - [bobko.todomail.util.LensTest]
 */
class Lens<A : Any, B>(val getter: (obj: A) -> B, val setter: (obj: A, newValue: B) -> A) {
    fun get(source: A): B = getter(source)
    fun set(source: A, newValue: B): A = setter(source, newValue)
}

inline val <reified A : Any, B> KProperty1<A, B>.lens: Lens<A, B>
    get() {
        require(A::class.isData)
        val property = this
        return Lens(property, { obj, newValue -> obj.copyAndChangeProperty(property, newValue) })
    }

fun <A : Any, B : Any, C : Any> Lens<A, B>.map(other: Lens<B, C>): Lens<A, C> = Lens(
    { obj -> other.getter(getter(obj)) },
    { obj, newValue -> setter(obj, other.setter(getter(obj), newValue)) }
)

inline fun <A : Any, reified B : Any, C : Any> Lens<A, B>.map(crossinline other: (B) -> KProperty0<C>): Lens<A, C> {
    require(B::class.isData)
    return this@map.map(
        Lens(
            { obj -> other(obj).get() },
            { obj, newValue -> obj.copyAndChangeProperty(other(obj), newValue) }
        )
    )
}

inline fun <reified A : Any, reified B : Any, C : Any> KProperty1<A, B>.map(crossinline other: (B) -> KProperty0<C>) =
    this.lens.map(other)
