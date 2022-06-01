package pl.bsk.chatapp.model

import java.io.Serializable
import java.time.LocalTime

open class Message(
    val sendTime: LocalTime,
    val content: String,
    var isMine: Boolean
):Serializable{
    fun sendTimeString():String = sendTime.toString().dropLast(4)
    override fun toString(): String {
        return "Message(sendTime=$sendTime, content='$content', isMine=$isMine)"
    }
}