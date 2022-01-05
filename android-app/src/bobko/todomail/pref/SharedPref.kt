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

import bobko.todomail.util.*
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class SharedPref<T : Any>(
    private val propertyReceiver: Any?
) : ReadOnlyProperty<Any?, SharedPref<T>> {
    fun PrefWriterDslReceiver.write(value: T?) {
        val normalized = normalize(value)
        writeImpl(normalized)
        _liveData.get()?.let { it.value = normalized ?: read() }
    }

    abstract fun PrefWriterDslReceiver.writeImpl(value: T?)
    abstract fun PrefReaderDslReceiver.read(): T

    open fun normalize(value: T?) = value

    private var _liveData = WeakReference<MutableInitializedLiveData<T>>(null)

    /**
     * this [SharedPref] must be a singleton for this feature to work
     */
    val PrefReaderDslReceiver.liveData: MutableInitializedLiveData<T>
        get() {
            val dispatchReceiver = this@SharedPref
            check(
                dispatchReceiver::class.objectInstance != null ||
                        propertyReceiver != null &&
                        propertyReceiver::class.objectInstance != null
            ) {
                "Shared pref should be singleton to be able to get liveData"
            }
            return _liveData.get() ?: mutableLiveDataOf(read()).also {
                _liveData = WeakReference(it)
            }
        }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = this
}
