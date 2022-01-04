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

package bobko.todomail.model

import com.github.kittinunf.fuel.jackson.defaultMapper
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import org.junit.Assert
import bobko.todomail.credential.sealed.GoogleOauth2TokenResponse
import bobko.todomail.credential.sealed.GmailMessageSendRequest
import org.junit.Test

class GoogleAuthJsonSerialization {

    @Test
    fun googleOauth2DeserializationTest() {
        val googleOauth2Deserializer = jacksonDeserializerOf<GoogleOauth2TokenResponse>()
        fun test(string: String, expected: GoogleOauth2TokenResponse) {
            Assert.assertEquals(expected, googleOauth2Deserializer.deserialize(string))
        }
        test("""{"access_token":"exp"}""", GoogleOauth2TokenResponse("exp"))
        test("""{"access_token":"exp", "refresh_token":"ref_exp"}""", GoogleOauth2TokenResponse("exp", "ref_exp"))
        test("""{"access_token":"exp", "something_else":"value"}""", GoogleOauth2TokenResponse("exp"))
    }

    @Test
    fun gmailMessageSerializationTest() {
        Assert.assertEquals("""{"raw":"expected"}""", defaultMapper.writeValueAsString(GmailMessageSendRequest("expected")))
    }
}
