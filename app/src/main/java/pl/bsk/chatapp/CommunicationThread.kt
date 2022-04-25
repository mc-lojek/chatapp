package pl.bsk.chatapp

import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket

class CommunicationThread(val clientSocket: Socket): Runnable {

    private lateinit var input: BufferedReader

    init {
        try {
            input = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        Timber.d("komunikacja 1")
        while(!Thread.currentThread().isInterrupted) {
            Timber.d("komunikacja 2")
            try {
                Timber.d("komunikacja 3")
                val read = input.readLine()
                Timber.d("address klienta: ${clientSocket.inetAddress.hostName} ${clientSocket.inetAddress.address} ${clientSocket.inetAddress.hostAddress}")
                Timber.d("przeczytalem cos takiego ${read}")

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}