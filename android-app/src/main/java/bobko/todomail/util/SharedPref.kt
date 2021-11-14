package bobko.todomail.util

import java.lang.ref.WeakReference
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class SharedPref<T : Any>(
    private val propertyReceiver: Any?
) : ReadOnlyProperty<Any?, SharedPref<T>> {
    abstract fun PrefWriterDslReceiver.write(value: T?)
    abstract fun PrefReaderDslReceiver.read(): T

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

private class StringBasedSharedPref<T : Any>(
    propertyReceiver: Any,
    val key: String,
    val defaultValue: T,
    val serialize: (T) -> String,
    val deserialize: (String) -> T,
) : SharedPref<T>(propertyReceiver) {
    override fun PrefWriterDslReceiver.write(value: T?) {
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
