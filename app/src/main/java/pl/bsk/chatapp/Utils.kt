package pl.bsk.chatapp

import android.util.Base64
import java.io.*
import java.util.Base64.getUrlEncoder

var MY_MESSAGE_INDEX_COUNTER = 0

fun Serializable.serialize(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(this)
    objectOutputStream.flush()
    val result = byteArrayOutputStream.toByteArray()
    byteArrayOutputStream.close()
    objectOutputStream.close()
    return result
}

fun ByteArray.deserialize(): Serializable {
    val byteArrayInputStream = ByteArrayInputStream(this)
    val objectInput = ObjectInputStream(byteArrayInputStream)
    val result = objectInput.readObject() as Serializable
    objectInput.close()
    byteArrayInputStream.close()
    return result
}

fun ByteArray.toBase64(): String =
    Base64.encodeToString(this, Base64.NO_WRAP)

fun String.fromBase64(): ByteArray =
    Base64.decode(this, Base64.NO_WRAP)