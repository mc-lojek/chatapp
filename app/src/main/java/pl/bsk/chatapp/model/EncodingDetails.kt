package pl.bsk.chatapp.model

import java.io.Serializable

class EncodingDetails(
    val encodingType:String,
    val sizeOfMessage:Int,
    val iv: ByteArray
) :Serializable{

}