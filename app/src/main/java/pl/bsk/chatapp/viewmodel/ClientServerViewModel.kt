package pl.bsk.chatapp.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.*
import pl.bsk.chatapp.model.FileMeta
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.min
import kotlin.system.measureTimeMillis


class ClientServerViewModel : ViewModel() {

    var serverAddress: String? = null
    private lateinit var server2ClientSocket: ServerSocket
    private lateinit var client2ServerSocket: Socket

    var isServerCommunicationSocketRunning = false

    private lateinit var oStream: OutputStream
    private lateinit var iStream: InputStream

    private val objectSizeBuffer = ByteArray(INT_SIZE)
    private val objectBuffer = ByteArray(OBJECT_CHUNK_SIZE)
    private val fileBuffer = ByteArray(FILE_CHUNK_SIZE)
    private val fileOutputBuffer = ByteArray(FILE_CHUNK_SIZE)

    private val _newMessageLiveData: MutableLiveData<Message?> = MutableLiveData(null)
    val newMessageLiveData = _newMessageLiveData as LiveData<Message?>

    fun listenServerConnection(action: (String) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            var socket: Socket?
            try {
                server2ClientSocket = ServerSocket(SERVER_PORT)
                try {
                    socket = server2ClientSocket.accept()
                    viewModelScope.launch { communicateToClient(socket, action) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun communicateToClient(clientSocket: Socket, action: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                iStream = clientSocket.getInputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isServerCommunicationSocketRunning = true
            while (isServerCommunicationSocketRunning) {
                try {
                    iStream.read(objectSizeBuffer, 0, INT_SIZE)
                    val objectSize = objectSizeBuffer.deserialize() as Int
                    Timber.d("po konwersji na inta ${objectSize}")

                    //todo dając tutaj sleepa pozwalamy zeby ten co wysyla nadazal pisac zanim my to cale przeczytamy

                    val foo = iStream.read(objectBuffer, 0, objectSize)

                    Timber.d("foo: ${foo}")

                    val obj = objectBuffer.deserialize()

                    when (obj) {
                        is Message -> {
                            obj.isMine = false
                            _newMessageLiveData.postValue(obj)
                            Timber.d("przeczytalem cos takiego ${obj.content}")
                        }
                        is FileMeta -> {
                            Timber.d("Dostałem meta pliku ${obj.filename} a jego rozmiar to ${obj.size}")
                            readFileFromClient(obj)
                        }
                        else -> {
                            Timber.e("Jakis inny typ niz powinien byc ?!")
                        }
                    }
                    if (serverAddress == null) {
                        serverAddress = clientSocket.inetAddress.hostName
                        connectToServer(serverAddress!!, action)
                    }
                } catch (e: IOException) {
                    Timber.d("tutaj sie wywalam")
                    e.printStackTrace()
                    isServerCommunicationSocketRunning = false
                }
            }
        }
    }

    private fun readFileFromClient(fileMeta: FileMeta) {
        Timber.d("zaczynam czytac plik")
        val fileOut = File("/sdcard/Download/${fileMeta.filename}")
        if (!fileOut.exists()) {
            fileOut.createNewFile();
        }
        val fos = FileOutputStream(fileOut)

        if (fileMeta.size <= FILE_CHUNK_SIZE) {
            // plik jest maly i zmiesci sie na raz
            Timber.d("czytam sobie")
            iStream.read(fileBuffer, 0, fileMeta.size.toInt())
            Timber.d("zapisuje do pliku")
            fos.write(fileBuffer, 0, fileMeta.size.toInt())
            Timber.d("skonczylem")
        } else {
            // duzy plik, trzeba chunkowac
            var received = 0
            var left = fileMeta.size.toInt()
            while (received < fileMeta.size) {
                Timber.d("rec: ${received} left: ${left}")
                Timber.d("odczytałem tyle bajtów: "+iStream.read(fileBuffer, 0, min(FILE_CHUNK_SIZE, left)))
                fos.write(fileBuffer, 0, min(FILE_CHUNK_SIZE, left))
                received += FILE_CHUNK_SIZE
                left -= FILE_CHUNK_SIZE
            }
        }
        fos.close()
    }

    fun connectToServer(serverAddress: String, action: (String) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                client2ServerSocket = Socket(serverAddr, SERVER_PORT)
                oStream = client2ServerSocket.getOutputStream()
                action("1")
            } catch (e: IOException) {
                e.printStackTrace()
                action(e.message ?: "Undefined error")
            }
        }
    }

    fun sendMessageToServer(msg: Message) = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            val array = msg.serialize()

            oStream.write(array.size.serialize(), 0, 81)
            oStream.write(array, 0, array.size)

            _newMessageLiveData.postValue(msg)

        }
    }

    fun sendFile(file: File) = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            val meta = FileMeta(file.name, file.length())

            val array = meta.serialize()

            oStream.write(array.size.serialize(), 0, 81)
            oStream.write(array, 0, array.size)
            val bis = BufferedInputStream(FileInputStream(file))

            if (meta.size <= FILE_CHUNK_SIZE) {
                bis.read(fileOutputBuffer, 0, meta.size.toInt())
                oStream.write(fileOutputBuffer, 0, meta.size.toInt())
            } else {
                var sent = 0
                var left = meta.size.toInt()
                //todo moznaby meta.size na inta zmienic z longa
                while (sent < meta.size) {
                    Timber.d("sent: ${sent} left: ${left}")
                    bis.read(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))
                    oStream.write(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))
                    sent += FILE_CHUNK_SIZE
                    left -= FILE_CHUNK_SIZE
                }
            }
        }
    }

    fun sendFile(uri: Uri, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            val nameAndSize = queryNameAndSize(context.contentResolver, uri)

            val meta = FileMeta(nameAndSize.first, nameAndSize.second)

            val array = meta.serialize()

            oStream.write(array.size.serialize(), 0, 81)
            oStream.write(array, 0, array.size)
            val bis = BufferedInputStream(context.getContentResolver().openInputStream(uri))

            if (meta.size <= FILE_CHUNK_SIZE) {
                bis.read(fileOutputBuffer, 0, meta.size.toInt())
                oStream.write(fileOutputBuffer, 0, meta.size.toInt())
            } else {
                var sent = 0
                var left = meta.size.toInt()
                //todo moznaby meta.size na inta zmienic z longa
                while (sent < meta.size) {
                    Timber.d("sent: ${sent} left: ${left}")
                    bis.read(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))
                    oStream.write(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))
                    sent += FILE_CHUNK_SIZE
                    left -= FILE_CHUNK_SIZE
                }
            }



            Timber.d("moze to jest nazwa? ${nameAndSize.first} a to rozmiar ${nameAndSize.second}")
        }
    }

    private fun queryNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        val returnCursor: Cursor = resolver.query(uri, null, null, null, null)!!
        val nameIndex: Int = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex: Int = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name: String = returnCursor.getString(nameIndex)
        val size: Long = returnCursor.getLong(sizeIndex)
        returnCursor.close()
        return Pair(name, size)
    }

//    @Throws(IOException::class)
//    private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
//        var fileSize = 0L
//        val inputStream: InputStream? = context.getContentResolver().openInputStream(uri)
//        if (inputStream != null) {
//            val bytes = ByteArray(1024)
//            var read = -1
//            while (inputStream.read(bytes).also { read = it } >= 0) {
//                fileSize += read.toLong()
//            }
//        }
//        inputStream?.close()
//        return fileSize
//    }
}