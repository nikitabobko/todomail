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
    final override fun PrefReaderDslReceiver.read() = readImpl(this)

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
    protected fun readImpl(receiver: PrefReaderDslReceiver) = with(receiver) {
        (0 until size.read()).map { itemSharedPref(it).read() }
    }
}
