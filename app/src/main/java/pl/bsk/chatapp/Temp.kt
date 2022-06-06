package pl.bsk.chatapp

import android.os.Environment
import pl.bsk.chatapp.model.EncodingDetails
import pl.bsk.chatapp.model.FileMeta
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*



    fun doStuff() {

        val encoding = EncodingDetails(CryptoManager.encodingType,100, CryptoManager.generateRandomIV()).serialize()
        Timber.d("Rozmiar z ustawionym type: ${encoding.size}")
        val encoding2 = EncodingDetails("nic",200, CryptoManager.generateRandomIV()).serialize()
        Timber.d("Rozmiar z NULL type: ${encoding2.size}")


    }
