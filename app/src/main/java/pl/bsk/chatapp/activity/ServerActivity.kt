package pl.bsk.chatapp.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pl.bsk.chatapp.R
import pl.bsk.chatapp.ServerThread

class ServerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        Thread(ServerThread()).start()
    }
}