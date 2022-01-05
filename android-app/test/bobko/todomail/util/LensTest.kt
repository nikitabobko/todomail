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
        Assert.assertEquals(expectedValue, Foo::bar.map { it::text }.set(init, expectedValue).bar.text)
        Assert.assertEquals(init.copy(bar = Bar(expectedValue)), Foo::bar.map { it::text }.set(init, expectedValue))
    }

    @Test
    fun testGet() {
        val expectedValue = "expected value"
        Assert.assertEquals(expectedValue, Foo::bar.map { it::text }.get(Foo(Bar(expectedValue))))
    }

    @Test
    fun testDataClassesAssert() {
        try {
            NotDataClass::text.lens
            fail()
        } catch (ignored: IllegalArgumentException) { }
        try {
            Foo::notDataClass.map { it::text }
            fail()
        } catch (ignored: IllegalArgumentException) { }
    }
}
