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

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.MutableLiveData
import kotlin.reflect.KProperty
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

data class TargetWithContext<T, C>(val target: T, val context: C) {
    override fun equals(other: Any?) = target == other?.safeCast<TargetWithContext<*, *>>()?.target
    override fun hashCode() = target.hashCode()
}

inline fun <T> T?.orElse(block: () -> T): T = this ?: block()

fun Context.showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.composeView(body: @Composable () -> Unit): View =
    ComposeView(this).apply { setContent(body) }

@Composable
fun <T : Any> MutableInitializedLiveData<T>.observeAsMutableState(): MutableState<T> {
    return liveData.observeAsMutableState { value }
}

@Composable
fun <T : Any> MutableLiveData<T>.observeAsMutableState(initial: () -> T): MutableState<T> {
    val mutableLiveData = this
    val state = observeAsState(value ?: initial())

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                mutableLiveData.value = value
            }

        override fun component1(): T = value
        override fun component2(): (T) -> Unit = { value = it }
    }
}

fun sign(x: Int): Int {
    return when {
        x < 0 -> -1
        x > 0 -> 1
        else -> 0
    }
}

val isTestMode =
    try {
        Class.forName("org.junit.Test")
        true
    } catch (ex: ClassNotFoundException) {
        false
    }

val doesSupportTiles: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

fun <T : Any, V> T.copyAndChangeProperty(property: KProperty<V>, value: V): T {
    require(this::class.isData)
    val copyMethod = this::class.memberFunctions.single { it.name == "copy" }
    val instanceParam = copyMethod.instanceParameter!!
    val parameterToAmend = copyMethod.parameters.single { it.name == property.name }
    return copyMethod.callBy(mapOf(instanceParam to this, parameterToAmend to value)) as T
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
inline fun <reified T : Any> Any.safeCast(): @kotlin.internal.NoInfer T? = this as? T

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
inline fun <reified T : Any> Any.cast(): @kotlin.internal.NoInfer T = this as T

fun errorException(throwable: Throwable): Nothing = throw IllegalStateException(throwable)
