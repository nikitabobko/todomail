/*
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

class PrefWriterDslReceiverForTest(private val impl: PrefWriterDslReceiverImpl) : PrefWriterDslReceiver by impl {
    private val existing = HashSet<String>()

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
