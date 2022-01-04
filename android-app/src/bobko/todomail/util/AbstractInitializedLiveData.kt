/*
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

typealias InitializedLiveData<T> = AbstractInitializedLiveData<T, LiveData<T>>

open class AbstractInitializedLiveData<T : Any, out LD : LiveData<T>>(val liveData: LD) {
    init {
        require(liveData.value != null) { "LiveData=$liveData must be initialized" }
    }
    open val value: T get() = liveData.value!!
}

class MutableInitializedLiveData<T : Any>(liveData: MutableLiveData<T>) :
    AbstractInitializedLiveData<T, MutableLiveData<T>>(liveData) {
    override var value: T
        get() = super.value
        set(value) {
            liveData.value = value
        }
}

fun <T : Any> mutableLiveDataOf(value: T): MutableInitializedLiveData<T> =
    MutableInitializedLiveData(MutableLiveData(value))

@Composable
fun <T : Any> AbstractInitializedLiveData<T, LiveData<T>>.observeAsState(): State<T> =
    liveData.observeAsState(value)
