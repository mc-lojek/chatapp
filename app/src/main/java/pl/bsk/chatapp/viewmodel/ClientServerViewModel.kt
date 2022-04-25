package pl.bsk.chatapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.CommunicationThread
import pl.bsk.chatapp.SERVER_PORT
import timber.log.Timber
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ClientServerViewModel : ViewModel() {

    var serverAddress: String? = null

    lateinit var server2ClientSocket: ServerSocket
    lateinit var client2ServerSocket: Socket
    var isServerSocketRunning = false
    var isServerCommunicationSocketRunning = false

    lateinit var inputFromClient: BufferedReader

    fun listenServerConnection() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            var socket: Socket?
            try {
                server2ClientSocket = ServerSocket(SERVER_PORT)

                isServerSocketRunning = true
                while (isServerSocketRunning) {
                    Timber.d("tu wszedlem server")
                    try {
                        Timber.d("tu wszedlem server 2")
                        socket = server2ClientSocket.accept()
                        viewModelScope.launch { communicateToClient(socket) }
                        Timber.d("tu wszedlem server3")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    suspend fun communicateToClient(clientSocket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                inputFromClient = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isServerCommunicationSocketRunning = true
            while (isServerCommunicationSocketRunning) {
                try {
                    Timber.d("komunikacja 3")
                    val read = inputFromClient.readLine()
                    if (serverAddress == null) {
                        serverAddress = clientSocket.inetAddress.hostName
                        //todo tu trzeba odpalic kolejny watek jako klient
                        // i zmienic fragment na chatFragment
                    }
                    Timber.d("przeczytalem cos takiego ${read}")

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun connectToServer(serverAddress: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                Timber.d("klient 1")
                client2ServerSocket = Socket(serverAddr, 8888)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessageToServer(msg: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            val output = PrintWriter(
                BufferedWriter(OutputStreamWriter(client2ServerSocket.getOutputStream())),
                true
            )
            output.println(msg)
        }
    }
}