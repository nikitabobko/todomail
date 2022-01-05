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
import bobko.todomail.util.orElse
import kotlin.properties.PropertyDelegateProvider

private class StringBasedSharedPref<T : Any>(
    propertyReceiver: Any,
    val key: String,
    val defaultValue: T,
    val serialize: (T) -> String,
    val deserialize: (String) -> T,
) : SharedPref<T>(propertyReceiver) {
    override fun PrefWriterDslReceiver.writeImpl(value: T?) {
        putString(key, value?.let(serialize))
    }

    override fun PrefReaderDslReceiver.read() =
        pref.getString(key, null).orElse { return defaultValue }.let(deserialize)
}

fun <T : Any> stringBasedSharedPref(
    defaultValue: T,
    serialize: (T) -> String,
    deserialize: (String) -> T,
    keySuffix: String?,
): PropertyDelegateProvider<Any, SharedPref<T>> =
    PropertyDelegateProvider { propertyReceiver, property ->
        val realKey = if (keySuffix != null) property.name + keySuffix else property.name
        require(keySuffix != null || propertyReceiver::class.objectInstance != null) {
            "'$realKey' shared pref should either have a suffix or belong to singleton"
        }
        StringBasedSharedPref(propertyReceiver, realKey, defaultValue, serialize, deserialize)
    }

fun stringSharedPref(defaultValue: String, keySuffix: String? = null) =
    stringBasedSharedPref(defaultValue, { it }, { it }, keySuffix)

fun intSharedPref(defaultValue: Int, keySuffix: String? = null) =
    stringBasedSharedPref(defaultValue, Int::toString, String::toInt, keySuffix)

fun booleanSharedPref(defaultValue: Boolean, keySuffix: String? = null) =
    stringBasedSharedPref(defaultValue, Boolean::toString, String::toBooleanStrict, keySuffix)
