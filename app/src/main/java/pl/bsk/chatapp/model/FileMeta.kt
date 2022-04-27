package pl.bsk.chatapp.model

import java.io.Serializable

class FileMeta(
    val filename: String,
    val size: Long,
): Serializable {
    override fun toString(): String {
        return "FileMeta(filename='$filename', size=$size)"
    }
}