package pl.bsk.chatapp.model

import java.io.Serializable
import javax.crypto.SecretKey

class EncodedSessionKeyAES(
    val encodedSessionKey:String
) :Serializable{
}