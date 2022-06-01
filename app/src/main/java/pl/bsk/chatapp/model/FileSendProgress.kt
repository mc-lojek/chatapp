package pl.bsk.chatapp.model

data class FileSendProgress(
    val totalFileSize: Int,
    val sentFileSize: Int,
    val errorMessage: String? = null
) {
    // progress percentage 0-100%
    val progress = (sentFileSize.toFloat() / totalFileSize.toFloat() * 100.0).toInt()
}