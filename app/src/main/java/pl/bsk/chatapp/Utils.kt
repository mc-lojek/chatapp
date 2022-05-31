package pl.bsk.chatapp

import java.io.*

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
