package pl.bsk.chatapp.model

import java.io.Serializable
import java.time.LocalTime

class Message(
    val sendTime: LocalTime,
    val content: String,
    val isMine: Boolean
):Serializable{
    fun sendTimeString():String = sendTime.toString().dropLast(4)
}