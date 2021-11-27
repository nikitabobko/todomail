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
