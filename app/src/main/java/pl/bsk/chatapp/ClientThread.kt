package pl.bsk.chatapp

import timber.log.Timber
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.lang.Exception
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class ClientThread : Runnable {

    lateinit var socket: Socket

    override fun run() {

        try {
            val serverAddr: InetAddress = InetAddress.getByName("192.168.0.192")
            Timber.d("klient 1")
            socket = Socket(serverAddr, 8888)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Timber.d("klient2")
        while (true) {
            try {
                Timber.d("klient 3")
                val output =
                    PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                output.println("siemano wysylam wiadomosc")
                Timber.d("klient 4")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }
}