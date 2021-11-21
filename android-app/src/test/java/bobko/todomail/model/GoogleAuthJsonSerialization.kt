package bobko.todomail.model

import com.github.kittinunf.fuel.jackson.defaultMapper
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import org.junit.Assert
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
