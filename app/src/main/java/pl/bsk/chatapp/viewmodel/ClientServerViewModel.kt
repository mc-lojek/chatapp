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
import pl.bsk.chatapp.model.*
import timber.log.Timber
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.time.LocalTime
import javax.crypto.Cipher
import kotlin.math.min


class ClientServerViewModel : ViewModel() {

    var serverAddress: String? = null
    private lateinit var server2ClientSocket: ServerSocket
    private lateinit var client2ServerSocket: Socket

    var isServerCommunicationSocketRunning = false

    private lateinit var oStream: OutputStream
    private lateinit var iStream: InputStream

    private lateinit var oResponseStream: OutputStream
    private lateinit var iResponseStream: InputStream

    private val objectSizeBuffer = ByteArray(ENCODING_SIZE)
    private val objectBuffer = ByteArray(OBJECT_CHUNK_SIZE)
    private val fileBuffer = ByteArray(FILE_CHUNK_SIZE)
    private val fileOutputBuffer = ByteArray(FILE_CHUNK_SIZE)
    private val responseBuffer = ByteArray(ENCODING_SIZE)

    private val _newMessageLiveData: MutableLiveData<Message?> = MutableLiveData(null)
    val newMessageLiveData = _newMessageLiveData as LiveData<Message?>

    private val _confirmationResponseLiveData: MutableLiveData<Int?> = MutableLiveData(null)
    val confirmationResponseLiveData = _confirmationResponseLiveData as LiveData<Int?>

    private val _fileSendingStatusLiveData: MutableLiveData<FileSendProgress?> =
        MutableLiveData(null)
    val fileSendingStatusLiveData = _fileSendingStatusLiveData as LiveData<FileSendProgress?>

    val messagesList: MutableList<Message> = ArrayList()

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

    fun connectToServer(serverAddress: String, amIInitiator: Boolean, action: (String) -> Unit) =
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Timber.d("lacze sie")
                try {
                    val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                    client2ServerSocket = Socket(serverAddr, SERVER_PORT)
                    oStream = client2ServerSocket.getOutputStream()
                    iResponseStream = client2ServerSocket.getInputStream()

                    if (amIInitiator) {
                        val keyPairRSASerialized =
                            PublicKeyRSA(CryptoManager.keyPairRSA.public).serialize()
                        Timber.d("Jestem inicjatorem połączenia i wysylam swoj klucz publiczny, ktory wyglada tak: ${CryptoManager.keyPairRSA.public.encoded.toBase64()}")
                        val encodingType = NONE_MODE

                        oStream.write(
                            EncodingDetails(
                                encodingType,
                                keyPairRSASerialized.size,
                                ByteArray(16)
                            ).serialize(), 0, ENCODING_SIZE
                        )
                        oStream.write(keyPairRSASerialized, 0, keyPairRSASerialized.size)
                    } else {
                        Timber.d("Tworze klucz sesyjny, szyfruje kluczem publicznym kolegi i wysylam")
                        //todo tworzenie kluczu sesyjnego i szyfrowanie go kluczem publicznym kolegi i wysylamy
                        CryptoManager.sessionKey = CryptoManager.generateSessionKey()
                        Timber.d("Wygenerowamy SessionKey wyglada tak: ${CryptoManager.sessionKey.encoded.toBase64()}")
                        val encryptedSession =
                            EncodedSessionKeyAES(CryptoManager.encryptSessionKeyWithPublicKey()).serialize()
                        val encodingType = NONE_MODE

                        oStream.write(
                            EncodingDetails(
                                encodingType,
                                encryptedSession.size,
                                ByteArray(16)
                            ).serialize(), 0, ENCODING_SIZE
                        )

                        oStream.write(encryptedSession, 0, encryptedSession.size)

                    }


                    Timber.d("zaczynam nasluchiwanie")
                    listenResponse()
                    action(ADDRESS_CONNECT_SUCCESSFUL)
                } catch (e: IOException) {
                    e.printStackTrace()
                    action(e.message ?: "Undefined error")
                }
            }
        }

    suspend fun communicateToClient(clientSocket: Socket, action: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                iStream = clientSocket.getInputStream()
                oResponseStream = clientSocket.getOutputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isServerCommunicationSocketRunning = true
            while (isServerCommunicationSocketRunning) {
                try {
                    //najpierw czytamy jednego inta który mowi nam jaki rozmiar ma obiekt ktory przyjdzie jako kolejny

                    iStream.read(objectSizeBuffer, 0, ENCODING_SIZE)
                    //val objectSize = objectSizeBuffer.deserialize() as Int

                    val objectEncodingDetails = try {
                        objectSizeBuffer.deserialize()
                    } catch (e: Exception) {
                        Timber.d("spadlem z rowerka przy deserializacji czegos")
                        continue
                    } as EncodingDetails

                    Timber.d("po konwersji na obiekt, rozmiar wiadomosci to: ${objectEncodingDetails.sizeOfMessage}")
                    Timber.d("A odebrany typ kodowania tu: ${objectEncodingDetails.encodingType}")

                    //nastepnie czytamy ten obiekt w calosci z socketa
                    iStream.read(objectBuffer, 0, objectEncodingDetails.sizeOfMessage)

                    //deserializujemy go i sprawdzamy jakiego jest typu
                    val obj = try {
                        objectBuffer.deserialize()
                    } catch (e: Exception) {
                        Timber.d("otrzymalem message, albo sie wywalilem")
                        CryptoManager.decryptMessage(
                            objectEncodingDetails.encodingType, objectBuffer
                                .copyOfRange(0, objectEncodingDetails.sizeOfMessage),
                            objectEncodingDetails.iv
                        )
                    }

                    when (obj) {
                        is Message -> {
                            obj.isMine = false
                            _newMessageLiveData.postValue(obj)
                            sendConfirmationResponse(obj.id)
                            Timber.d("przeczytalem taka wiadomosc ${obj.content}")
                        }
                        is FileMeta -> {
                            Timber.d("Dostałem meta pliku ${obj.filename} a jego rozmiar to ${obj.size}")
                            readFileFromClient(obj)
                            sendConfirmationResponse(obj.id)
                        }
                        is PublicKeyRSA -> {
                            Timber.d("Dostalem klucz publiczny mojego kolegi i wyglada tak: ${obj.publicKey.encoded.toBase64()}")
                            CryptoManager.partnerPublicKey = obj.publicKey
                        }
                        is EncodedSessionKeyAES -> {
                            Timber.d("Dostalem zaszyfrowany klucz sesyjny: ${obj.encodedSessionKey}")
                            CryptoManager.decryptSessionKeyWithPrivateKey(obj.encodedSessionKey)
                            Timber.d("SessionKey po odszyfrowaniu wyglada tak: ${CryptoManager.sessionKey.encoded.toBase64()}")
                        }
                        else -> {


                        }
                    }
                    if (serverAddress == null) {
                        serverAddress = clientSocket.inetAddress.hostName
                        connectToServer(serverAddress!!, false, action)

                    }

                } catch (e: IOException) {
                    Timber.d("tutaj sie wywalam")
                    e.printStackTrace()
                    //isServerCommunicationSocketRunning = false
                }
            }
        }
    }

    private suspend fun sendConfirmationResponse(messageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("wysylam id wiadomosci ${messageId} ktora odebralem")
            oResponseStream.write(messageId.serialize(), 0, INT_SIZE)
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

            Timber.d("a taki sie odebral ${fileBuffer.toBase64()}")

            fos.write(fileBuffer, 0, got)
            received += got
            left -= got
            Timber.d("rec: ${received}, left: ${left}, got: ${got}")
        }
        val uri = Uri.parse(fileOut.path)
        _newMessageLiveData.postValue(
            FileMessage(
                LocalTime.now(),
                fileMeta.filename,
                false,
                -1,
                false,
                uri
            )
        )

        fos.close()
    }

    private suspend fun listenResponse() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    //najpierw czytamy jednego inta który mowi nam jaki rozmiar ma obiekt ktory przyjdzie jako kolejny
                    Timber.d("czekam na potwierdzenie")
                    iResponseStream.read(responseBuffer, 0, INT_SIZE)

                    val deliveredMessageId = try {
                        responseBuffer.deserialize()
                    } catch (e: Exception) {
                        Timber.d("spadlem z rowerka przy deserializacji czegos")
                        continue
                    } as Int

                    Timber.d("dostalem potwierdzenie takiej wiadomosci ${deliveredMessageId}")
                    _confirmationResponseLiveData.postValue(deliveredMessageId)

                } catch (e: IOException) {
                    Timber.d("tutaj sie wywalam")
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessageToServer(msg: Message) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val encodingType = CryptoManager.encodingType
            Timber.d("Aktualny typ kodowania to: $encodingType")

            val iv = CryptoManager.generateRandomIV()

            val array = CryptoManager.encryptMessage(encodingType, msg, iv)
            oStream.write(
                EncodingDetails(encodingType, array.size, iv).serialize(),
                0,
                ENCODING_SIZE
            )
            oStream.write(array, 0, array.size)

            _newMessageLiveData.postValue(msg)

        }
    }

    fun sendFile(uri: Uri, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {

            //TODO nie moze byc kropki w nazwie pliku
            val nameAndSize = queryNameAndSize(context.contentResolver, uri)
            val meta = FileMeta(
                nameAndSize.first.replace(" ", ""),
                nameAndSize.second.toInt(),
                MY_MESSAGE_INDEX_COUNTER
            )
            MY_MESSAGE_INDEX_COUNTER++

            val iv = CryptoManager.generateRandomIV()

            val encodingType = CryptoManager.encodingType
            val array = meta.serialize()
            oStream.write(
                EncodingDetails(encodingType, array.size, iv).serialize(),
                0,
                ENCODING_SIZE
            )
            oStream.write(array, 0, array.size)
            val bis = BufferedInputStream(context.contentResolver.openInputStream(uri))

            var got = 0
            var sent = 0
            var left = meta.size

            val cipher = Cipher.getInstance("AES/${CryptoManager.encodingType}/PKCS5PADDING")
            cipher.init(Cipher.ENCRYPT_MODE, CryptoManager.sessionKey)

            while (sent < meta.size) {
                got = bis.read(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))

                val temp = cipher.update(fileOutputBuffer, 0, got)
                Timber.d("taki fragmencik sie zakodowal ${temp.toBase64()}")

                oStream.write(temp, 0, got)
                sent += got
                left -= got
                Timber.d("sent: ${sent}, left: ${left}, got: $got")
                _fileSendingStatusLiveData.postValue(
                    FileSendProgress(meta.size, sent)
                )
            }
            _fileSendingStatusLiveData.postValue(null)
            _newMessageLiveData.postValue(Message(LocalTime.now(), meta.filename, true, meta.id))

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