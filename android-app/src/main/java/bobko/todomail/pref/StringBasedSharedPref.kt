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
        editor.putString(key, value?.let(serialize))
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
