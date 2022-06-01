package pl.bsk.chatapp.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.*
import pl.bsk.chatapp.model.FileMessage
import pl.bsk.chatapp.model.FileMeta
import pl.bsk.chatapp.model.FileSendProgress
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.LocalTime
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

    private val _fileSendingStatusLiveData: MutableLiveData<FileSendProgress?> =
        MutableLiveData(null)
    val fileSendingStatusLiveData = _fileSendingStatusLiveData as LiveData<FileSendProgress?>

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
                    //najpierw czytamy jednego inta który mowi nam jaki rozmiar ma obiekt ktory przyjdzie jako kolejny

                    iStream.read(objectSizeBuffer, 0, INT_SIZE)
                    //val objectSize = objectSizeBuffer.deserialize() as Int

                    val objectSize = try {
                        objectSizeBuffer.deserialize()
                    } catch (e: Exception) {
                        Timber.d("spadlem z rowerka przy deserializacji czegos")
                        continue
                    } as Int

                    Timber.d("po konwersji na inta ${objectSize}")

                    //nastepnie czytamy ten obiekt w calosci z socketa
                    iStream.read(objectBuffer, 0, objectSize)

                    //deserializujemy go i sprawdzamy jakiego jest typu
                    val obj = try {
                        objectBuffer.deserialize()
                    } catch (e: Exception) {
                        Timber.d("spadlem z rowerka przy deserializacji czegos")
                    }

                    when (obj) {
                        is Message -> {
                            obj.isMine = false
                            _newMessageLiveData.postValue(obj)
                            Timber.d("przeczytalem taka wiadomosc ${obj.content}")
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
                    //isServerCommunicationSocketRunning = false
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


        // duzy plik, trzeba chunkowac
        var received = 0
        var left = fileMeta.size
        var got = 0
        while (received < fileMeta.size) {
            got = iStream.read(fileBuffer, 0, min(FILE_CHUNK_SIZE, left))
            fos.write(fileBuffer, 0, got)
            received += got
            left -= got
            Timber.d("rec: ${received}, left: ${left}, got: ${got}")
        }
        val uri = Uri.parse(fileOut.path)
        _newMessageLiveData.postValue(FileMessage(LocalTime.now(), fileMeta.filename, false, uri))

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

    fun sendFile(uri: Uri, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            //TODO nie moze byc kropki w nazwie pliku
            val nameAndSize = queryNameAndSize(context.contentResolver, uri)
            val meta = FileMeta(nameAndSize.first.replace(" ",""), nameAndSize.second.toInt())

            val array = meta.serialize()
            oStream.write(array.size.serialize(), 0, 81)
            oStream.write(array, 0, array.size)
            val bis = BufferedInputStream(context.contentResolver.openInputStream(uri))

            var got = 0
            var sent = 0
            var left = meta.size

            while (sent < meta.size) {
                got = bis.read(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))
                oStream.write(fileOutputBuffer, 0, got)
                sent += got
                left -= got
                Timber.d("sent: ${sent}, left: ${left}, got: $got")
                _fileSendingStatusLiveData.postValue(
                    FileSendProgress(meta.size, sent)
                )
            }
            _fileSendingStatusLiveData.postValue(null)

        }
    }

    // gets filename and its size based on the uri and returns it as a pair
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
}