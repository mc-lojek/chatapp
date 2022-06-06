package pl.bsk.chatapp

import android.os.Environment
import pl.bsk.chatapp.model.EncodingDetails
import pl.bsk.chatapp.model.FileMeta
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*



    fun doStuff() {
//        val file = 2147483647//Message(LocalTime.now(),"cokolwgfjdfgjgfjgjhgjgjjgjggjgjjiek.pdf.jpgś", false)
//
//        Timber.d("przed serializacja: ${file}")
//        //val arr = serialize(file)
//        val arr = file.serialize()
//        Timber.d("po serializacji ma taki rozmiar ${arr.size} a tak wyglada ${arr}")
//        //val deserialized = deserialize<FileMeta>(arr)
//        arr.forEach { Timber.d(it.toString()) }
//        val deserialized = arr.deserialize()
//        Timber.d("po deserializacji: ${deserialized}")
//        when (deserialized) {
//            is Message -> {
//                Timber.d("jest message taki ${deserialized} a content taki ${deserialized.content}")
//            }
//            is FileMeta -> {
//                Timber.d("jest filemeta taki ${deserialized} a filename taki ${deserialized.filename}")
//            }
//            else -> {
//                Timber.e("Jakis inny typ niz powinien byc ?!")
//
//            }
//        }

//        val msg = Message(LocalTime.now(), "gowno", false)
//        Timber.d("przed serializacja: ${msg}")
//        val arr2 = serialize(msg)
//        Timber.d("po serializacji ma taki rozmiar ${arr2.size} a tak wyglada ${arr2}")
//        val deserialized2 = deserialize<Message>(arr2)
//        Timber.d("po deserializacji: ${deserialized2}")
//
//        val int = 44
//        Timber.d("przed serializacja: ${int}")
//        val arr3 = int.serialize()
//        Timber.d("po serializacji ma taki rozmiar ${arr3.size} a tak wyglada ${arr3}")
//        val deserialized3 = arr3.deserialize() as Int
//        Timber.d("po deserializacji: ${deserialized3}")


        val encoding = EncodingDetails(CryptoManager.encodingType,100).serialize()
        Timber.d("Rozmiar z ustawionym type: ${encoding.size}")
        val encoding2 = EncodingDetails("nic",200).serialize()
        Timber.d("Rozmiar z NULL type: ${encoding2.size}")


    }
