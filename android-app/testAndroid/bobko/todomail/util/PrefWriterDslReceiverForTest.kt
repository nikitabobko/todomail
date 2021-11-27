package bobko.todomail.util

class PrefWriterDslReceiverForTest(private val impl: PrefWriterDslReceiverImpl) : PrefWriterDslReceiver by impl {
    val existing = HashSet<String>()

    override fun putString(key: String, value: String?) {
        if (checkDuplicates && !existing.add(key)) {
            println("Duplicated key='$key'!")
        }
        impl.putString(key, value)
    }

    companion object {
        var checkDuplicates = false
    }
}
