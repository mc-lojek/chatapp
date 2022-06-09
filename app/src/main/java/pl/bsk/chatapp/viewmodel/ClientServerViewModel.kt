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
import javax.crypto.spec.IvParameterSpec
import kotlin.math.min
import kotlin.system.measureNanoTime


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
                try {
                    val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                    client2ServerSocket = Socket(serverAddr, SERVER_PORT)
                    oStream = client2ServerSocket.getOutputStream()
                    iResponseStream = client2ServerSocket.getInputStream()

                    if (amIInitiator) {
                        val keyPairRSASerialized =
                            PublicKeyRSA(CryptoManager.keyPairRSA.public).serialize()
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
                        //todo tworzenie kluczu sesyjnego i szyfrowanie go kluczem publicznym kolegi i wysylamy
                        CryptoManager.sessionKey = CryptoManager.generateSessionKey()
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
                    val start = System.nanoTime()
                    //czytamy obiekt, ktory ma w sobie informacje o typie kodowania, rozmiarze wiadomosci i wektor iv
                    iStream.read(objectSizeBuffer, 0, ENCODING_SIZE)

                    val objectEncodingDetails = try {
                        CryptoManager.decryptEncodingDetails(objectSizeBuffer)
                    } catch (e: UninitializedPropertyAccessException) {
                        objectSizeBuffer.deserialize() as EncodingDetails
                    } catch (e: Exception) {
                        Timber.e(e)
                        continue
                    }

                    //nastepnie czytamy wlasciwy obiekt w calosci z socketa
                    iStream.read(objectBuffer, 0, objectEncodingDetails.sizeOfMessage)

                    //deserializujemy go i sprawdzamy jakiego jest typu
                    val obj = try {
                        objectBuffer.deserialize()
                    } catch (e: Exception) {
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
                            val time = System.nanoTime() - start
                            Timber.d("receiving message with size ${obj.content.length} with time ${time}")
                        }
                        is FileMeta -> {
                            readFileFromClient(obj, objectEncodingDetails)
                            sendConfirmationResponse(obj.id)
                            val time = System.nanoTime() - start
                            Timber.d("receiving fileMeta with time ${time}")
                        }
                        is PublicKeyRSA -> {
                            CryptoManager.partnerPublicKey = obj.publicKey
                        }
                        is EncodedSessionKeyAES -> {
                            CryptoManager.decryptSessionKeyWithPrivateKey(obj.encodedSessionKey)
                        }
                        else -> {
                        }
                    }
                    if (serverAddress == null) {
                        serverAddress = clientSocket.inetAddress.hostName
                        connectToServer(serverAddress!!, false, action)

                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun sendConfirmationResponse(messageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            oResponseStream.write(messageId.serialize(), 0, INT_SIZE)
        }
    }

    private fun readFileFromClient(fileMeta: FileMeta, objectEncodingDetails: EncodingDetails) {

        val time = measureNanoTime {

            val fileOut = File("/sdcard/Download/${fileMeta.filename}")
            if (!fileOut.exists()) {
                fileOut.createNewFile();
            }
            val fos = FileOutputStream(fileOut)


            val cipher =
                Cipher.getInstance("AES/${objectEncodingDetails.encodingType}/PKCS5PADDING")
            if (objectEncodingDetails.encodingType == "ECB")
                cipher.init(Cipher.DECRYPT_MODE, CryptoManager.sessionKey)
            else {
                val ivParams = IvParameterSpec(objectEncodingDetails.iv)
                cipher.init(Cipher.DECRYPT_MODE, CryptoManager.sessionKey, ivParams)
            }


            val metaToBeReceived = fileMeta.size - 16
            // duzy plik, trzeba chunkowac
            var received = 0
            var left = metaToBeReceived
            var got = 0
            while (received < metaToBeReceived) {
                got = iStream.read(fileBuffer, 0, min(FILE_CHUNK_SIZE, left))

                if (got == FILE_CHUNK_SIZE) {
                    //otrzymalismy calosc, mozemy odszyfrowac
                } else if (left < FILE_CHUNK_SIZE) {
                    //koncowka musi byc pelna
                    while (got < left) {
                        var gotNext = iStream.read(fileBuffer, got, left - got)
                        got += gotNext
                    }
                } else {
                    while (got < FILE_CHUNK_SIZE) {
                        var gotNext = iStream.read(fileBuffer, got, FILE_CHUNK_SIZE - got)
                        got += gotNext
                    }
                    //pobieramy tyle ile nam brakuje do skutku
                    //dostalismy mniej, trzeba zrobic szpagat
                }
                //w tym momencie jestesmy gotowi odszyfrowac plik
                //odszyfrowywanie:
                val temp = cipher.update(fileBuffer, 0, got)

                fos.write(temp, 0, temp.size)
                received += got
                left -= got
            }
            //tu musi byc final na 16
            got = iStream.read(fileBuffer, 0, 16)
            val temp = cipher.doFinal(fileBuffer, 0, got)
            fos.write(temp, 0, temp.size)
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
        Timber.d("receiving file with size ${fileMeta.size} with time ${time}")
    }

    private suspend fun listenResponse() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    //najpierw czytamy jednego inta który mowi nam jaki rozmiar ma obiekt ktory przyjdzie jako kolejny
                    iResponseStream.read(responseBuffer, 0, INT_SIZE)

                    val deliveredMessageId = try {
                        responseBuffer.deserialize()
                    } catch (e: Exception) {
                        continue
                    } as Int

                    _confirmationResponseLiveData.postValue(deliveredMessageId)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendMessageToServer(msg: Message) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val time = measureNanoTime {
                val encodingType = CryptoManager.encodingType

                val iv = CryptoManager.generateRandomIV()

                //szyfrujemy tresc wiadomosci
                val array = CryptoManager.encryptMessage(encodingType, msg, iv)

                //szyfrowanie EncodingDetails
                val encodedDetails =
                    CryptoManager.encryptEncodingDetails(
                        EncodingDetails(
                            encodingType,
                            array.size,
                            iv
                        )
                    )
                //wysylamy EncodingDetails
                oStream.write(
                    encodedDetails,
                    0,
                    ENCODING_SIZE
                )
                //wysylamy zaszyfrowana wiadomosc
                oStream.write(array, 0, array.size)

                _newMessageLiveData.postValue(msg)
            }
            Timber.d("sending message with size ${msg.content.length} with time ${time}")
        }
    }

    fun sendFile(uri: Uri, context: Context) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val time = measureNanoTime {
                //TODO nie moze byc kropki w nazwie pliku
                val nameAndSize = queryNameAndSize(context.contentResolver, uri)
                val sizeEncrypted =
                    nameAndSize.second.toInt() - (nameAndSize.second.toInt() % 16) + 16
                val meta = FileMeta(
                    nameAndSize.first.replace(" ", ""),
                    sizeEncrypted,
                    MY_MESSAGE_INDEX_COUNTER
                )
                MY_MESSAGE_INDEX_COUNTER++
                val iv = CryptoManager.generateRandomIV()

                val encodingType = CryptoManager.encodingType

                val array = meta.serialize()


                //szyfrowanie encoding details
                val encodedDetails = CryptoManager.encryptEncodingDetails(
                    EncodingDetails(
                        encodingType,
                        array.size,
                        iv
                    )
                )


                oStream.write(
                    encodedDetails,
                    0,
                    ENCODING_SIZE
                )
                oStream.write(array, 0, array.size)
                val bis = BufferedInputStream(context.contentResolver.openInputStream(uri))

                var got = 0
                var sent = 0
                var left = meta.size - 16

                val cipher = Cipher.getInstance("AES/${CryptoManager.encodingType}/PKCS5PADDING")
                if (CryptoManager.encodingType == "ECB")
                    cipher.init(Cipher.ENCRYPT_MODE, CryptoManager.sessionKey)
                else {
                    val ivParams = IvParameterSpec(iv)
                    cipher.init(Cipher.ENCRYPT_MODE, CryptoManager.sessionKey, ivParams)
                }

                val metaSizeToSent = meta.size - 16
                while (sent < metaSizeToSent) {
                    got = bis.read(fileOutputBuffer, 0, min(FILE_CHUNK_SIZE, left))

                    val temp = cipher.update(fileOutputBuffer, 0, got)
                    oStream.write(temp, 0, got)
                    sent += got
                    left -= got
                    _fileSendingStatusLiveData.postValue(
                        FileSendProgress(metaSizeToSent, sent)
                    )
                }
                got = bis.read(fileOutputBuffer, 0, 16)
                var temp = cipher.update(fileOutputBuffer, 0, got)
                temp = cipher.doFinal()
                oStream.write(temp, 0, temp.size)



                _fileSendingStatusLiveData.postValue(null)
                _newMessageLiveData.postValue(
                    Message(
                        LocalTime.now(),
                        meta.filename,
                        true,
                        meta.id
                    )
                )
                Timber.d("sending file with size: ${meta.size}")
            }
            Timber.d("sending file with time: $time")
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