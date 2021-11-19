package bobko.todomail.util

import junit.framework.Assert.fail
import org.junit.Assert
import org.junit.Test

/**
 * Test for [Lens]
 */
class LensTest {
    data class Foo(val bar: Bar, val notDataClass: NotDataClass = NotDataClass(""))
    data class Bar(val text: String)

    class NotDataClass(val text: String)

    @Test
    fun testSet() {
        val expectedValue = "expected value"
        val init = Foo(Bar(""))
        Assert.assertEquals(expectedValue, Foo::bar.map { ::text }.set(init, expectedValue).bar.text)
        Assert.assertEquals(init.copy(bar = Bar(expectedValue)), Foo::bar.map { ::text }.set(init, expectedValue))
    }

    @Test
    fun testGet() {
        val expectedValue = "expected value"
        Assert.assertEquals(expectedValue, Foo::bar.map { ::text }.get(Foo(Bar(expectedValue))))
    }

    @Test
    fun testDataClassesAssert() {
        try {
            NotDataClass::text.lens
            fail()
        } catch (ignored: IllegalArgumentException) { }
        try {
            Foo::notDataClass.map { ::text }
            fail()
        } catch (ignored: IllegalArgumentException) { }
    }
}
