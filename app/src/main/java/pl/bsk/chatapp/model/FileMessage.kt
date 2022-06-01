package pl.bsk.chatapp.model

import android.net.Uri
import java.time.LocalTime

class FileMessage(
    sendTime: LocalTime,
    content: String,
    isMine: Boolean,
    id: Int,
    isRead: Boolean,
    val uri: Uri,
) : Message(sendTime, content, isMine, id, isRead) {
}