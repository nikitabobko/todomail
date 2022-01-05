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

package bobko.todomail.pref

import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver

open class ListSharedPref<T : Any>(
    propertyReceiver: Any?,
    uniqueSuffix: String,
    private val itemSharedPref: (index: Int) -> SharedPref<T>,
) : SharedPref<List<T>>(propertyReceiver) {
    private val size by intSharedPref(0, uniqueSuffix)

    final override fun PrefWriterDslReceiver.writeImpl(value: List<T>?) = writeList(this, value)
    final override fun PrefReaderDslReceiver.read() = readList(this)

    // https://youtrack.jetbrains.com/issue/KT-11488
    protected open fun writeList(dslReceiver: PrefWriterDslReceiver, value: List<T>?) = with(dslReceiver) {
        (0 until size.read()).forEach {
            itemSharedPref(it).write(null)
        }
        value?.forEachIndexed { index, item ->
            itemSharedPref(index).write(item)
        }
        size.write(value?.size)
    }

    // https://youtrack.jetbrains.com/issue/KT-11488
    protected open fun readList(receiver: PrefReaderDslReceiver) = with(receiver) {
        (0 until size.read()).map { itemSharedPref(it).read() }
    }
}
