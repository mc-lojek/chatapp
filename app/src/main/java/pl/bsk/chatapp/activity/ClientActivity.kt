package pl.bsk.chatapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.R
import timber.log.Timber
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetAddress
import java.net.Socket

class ClientActivity : AppCompatActivity() {

    lateinit var socket: Socket


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        //Thread(ClientThread()).start()

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val serverAddr: InetAddress = InetAddress.getByName("192.168.0.192")
                    Timber.d("klient 1")
                    socket = Socket(serverAddr, 8888)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        setupOnClicks()
    }

    private fun setupOnClicks() {
        findViewById<Button>(R.id.send_btn).setOnClickListener {

            val message = findViewById<EditText>(R.id.edit_text).text.toString()

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val output = PrintWriter(
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream())),
                        true
                    )
                    output.println(message)
                }
            }
        }
    }
}