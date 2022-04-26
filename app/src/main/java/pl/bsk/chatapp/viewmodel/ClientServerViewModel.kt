package pl.bsk.chatapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.SERVER_PORT
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ClientServerViewModel : ViewModel() {

    private var serverAddress: String? = null
    private lateinit var server2ClientSocket: ServerSocket
    private lateinit var client2ServerSocket: Socket

    var isServerCommunicationSocketRunning = false

    private lateinit var oos: ObjectOutputStream
    private lateinit var ois: ObjectInputStream

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
                ois = ObjectInputStream(clientSocket.getInputStream())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isServerCommunicationSocketRunning = true
            while (isServerCommunicationSocketRunning) {
                try {
                    when (val receivedObject = ois.readObject()) {
                        is Message -> {
                            receivedObject.isMine = false
                            _newMessageLiveData.postValue(receivedObject)
                            Timber.d("przeczytalem cos takiego ${receivedObject.content}")
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
                    e.printStackTrace()
                }
            }
        }
    }

    fun connectToServer(serverAddress: String, action: (String) -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                client2ServerSocket = Socket(serverAddr, 8888)
                oos = ObjectOutputStream(client2ServerSocket.getOutputStream())
                action("1")
            } catch (e: IOException) {
                e.printStackTrace()
                action(e.message ?: "Undefined error")
            }
        }
    }

    fun sendMessageToServer(msg: Message) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            oos.writeObject(msg)
            _newMessageLiveData.postValue(msg)
            oos.flush()
        }
    }
}