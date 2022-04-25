package pl.bsk.chatapp

import timber.log.Timber
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerThread: Runnable {

    lateinit var serverSocket: ServerSocket

    override fun run() {
        var socket: Socket?
        try {
            serverSocket = ServerSocket(8888)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        while(!Thread.currentThread().isInterrupted) {
            Timber.d("tu wszedlem server")
            try {
                Timber.d("tu wszedlem server 2")
                socket = serverSocket.accept()
                Thread(CommunicationThread(socket)).start()
                Timber.d("tu wszedlem server3")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }
}