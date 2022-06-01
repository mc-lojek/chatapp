package pl.bsk.chatapp.model

import java.io.Serializable

class FileMeta(
    val filename: String,
    val size: Int,
    val id: Int,
): Serializable {
    override fun toString(): String {
        return "FileMeta(filename='$filename', size=$size)"
    }
}