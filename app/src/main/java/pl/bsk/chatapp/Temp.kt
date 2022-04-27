package pl.bsk.chatapp

import android.os.Environment
import pl.bsk.chatapp.model.FileMeta
import pl.bsk.chatapp.model.Message
import timber.log.Timber
import java.io.*


fun Serializable.serialize(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream: ObjectOutputStream
        objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(this)
        objectOutputStream.flush()
        val result = byteArrayOutputStream.toByteArray()
        byteArrayOutputStream.close()
        objectOutputStream.close()
        return result
    }

    fun ByteArray.deserialize(): Serializable {
        val byteArrayInputStream = ByteArrayInputStream(this)
        val objectInput: ObjectInput
        objectInput = ObjectInputStream(byteArrayInputStream)
        val result = objectInput.readObject() as Serializable
        objectInput.close()
        byteArrayInputStream.close()
        return result
    }

    fun doStuff() {
        val file = 2147483647//Message(LocalTime.now(),"cokolwgfjdfgjgfjgjhgjgjjgjggjgjjiek.pdf.jpgś", false)

        Timber.d("przed serializacja: ${file}")
        //val arr = serialize(file)
        val arr = file.serialize()
        Timber.d("po serializacji ma taki rozmiar ${arr.size} a tak wyglada ${arr}")
        //val deserialized = deserialize<FileMeta>(arr)
        arr.forEach { Timber.d(it.toString()) }
        val deserialized = arr.deserialize()
        Timber.d("po deserializacji: ${deserialized}")
        when (deserialized) {
            is Message -> {
                Timber.d("jest message taki ${deserialized} a content taki ${deserialized.content}")
            }
            is FileMeta -> {
                Timber.d("jest filemeta taki ${deserialized} a filename taki ${deserialized.filename}")
            }
            else -> {
                Timber.e("Jakis inny typ niz powinien byc ?!")

            }
        }

//        val msg = Message(LocalTime.now(), "gowno", false)
//        Timber.d("przed serializacja: ${msg}")
//        val arr2 = serialize(msg)
//        Timber.d("po serializacji ma taki rozmiar ${arr2.size} a tak wyglada ${arr2}")
//        val deserialized2 = deserialize<Message>(arr2)
//        Timber.d("po deserializacji: ${deserialized2}")
//
//        val int = 44
//        Timber.d("przed serializacja: ${int}")
//        val arr3 = serialize(int)
//        Timber.d("po serializacji ma taki rozmiar ${arr3.size} a tak wyglada ${arr3}")
//        val deserialized3 = deserialize<Long>(arr3)
//        Timber.d("po deserializacji: ${deserialized3}")
    }

    fun readFile() {
        val file = File("/sdcard/Download/file.txt")
        if (!file.exists()) {
            file.createNewFile();
        }
        val bytes = ByteArray(file.length().toInt())
        val bis = BufferedInputStream(FileInputStream(file))
        bis.read(bytes, 0, bytes.size)
        Timber.d("przeczytalem")
        val fileOut = File("/sdcard/Download/fileOut.txt")
        val fos = FileOutputStream(fileOut)
        fos.write(bytes, 0, bytes.size)
        Timber.d("Zapisalem")
        fos.close()
        bis.close()
    }
