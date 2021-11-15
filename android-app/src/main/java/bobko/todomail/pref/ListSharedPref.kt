package bobko.todomail.pref

import bobko.todomail.util.PrefReaderDslReceiver
import bobko.todomail.util.PrefWriterDslReceiver

open class ListSharedPref<T : Any>(
    propertyReceiver: Any?,
    uniqueSuffix: String,
    private val itemSharedPref: (index: Int) -> SharedPref<T>,
) : SharedPref<List<T>>(propertyReceiver) {
    private val size by intSharedPref(0, uniqueSuffix)

    override fun PrefWriterDslReceiver.write(value: List<T>?) {
        (0 until size.read()).forEach {
            itemSharedPref(it).write(null)
        }
        value?.forEachIndexed { index, item ->
            itemSharedPref(index).write(item)
        }
        size.write(value?.size)
    }

    override fun PrefReaderDslReceiver.read() =
        (0 until size.read()).map { itemSharedPref(it).read() }
}
