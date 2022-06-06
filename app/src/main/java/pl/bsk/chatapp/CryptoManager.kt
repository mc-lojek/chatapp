package pl.bsk.chatapp

import android.content.SharedPreferences
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.io.Serializable
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private lateinit var prefs: SharedPreferences
    lateinit var sessionKey: SecretKey
    lateinit var partnerPublicKey: PublicKey
    lateinit var keyPairRSA: KeyPair
    var encodingType: String = ECB_MODE

    fun initialize(prefs: SharedPreferences) {
        this.prefs = prefs

    }

    fun login(insertedPassword: String): Boolean {
        val storedHash = prefs.getString(PASSWORD_HASH_KEY, "") ?: ""
        val insertedHash = hashPassword(insertedPassword)

        Timber.d("podane z klawiatury: ${insertedHash}koniec")
        Timber.d("zapisane w prefsach: ${storedHash}koniec")

        if (storedHash.equals("")) {
            register(insertedHash)
            val keys = generateRSAKeyPair()
            val encodedKeys = encodeRSAKeys(keys, insertedHash)
            saveRSAKeysLocally(encodedKeys.first, encodedKeys.second)
            keyPairRSA = keys
            return true

        } else if (storedHash.trim().equals(insertedHash.trim())) {
            Timber.d("haslo poprawne, chodzmy dalej")
            val newKeysFromMemory = getRSAKeysFromMemory()
            keyPairRSA = decodeRSAKeys(insertedHash, newKeysFromMemory)
            return true

        } else {
            //Toast.makeText(requireContext(), "Podales zle haslo!", Toast.LENGTH_LONG).show()
            //todo przechodzi dalej ale pluje śmieci
            return false
        }
    }

    private fun register(hash: String) {
        with(prefs.edit()) {
            putString(PASSWORD_HASH_KEY, hash)
            apply()
        }
    }


    fun hashPassword(password: String): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val encodedhash: ByteArray = digest.digest(
            password.encodeToByteArray()
        )

        Timber.d("takie jest haslo do zaszyfrowania: ${password}")
        Timber.d("taka jest byte array po zaszyfrowaniu: ${encodedhash.toBase64()}")


        return encodedhash.toBase64()
    }

    fun generateRSAKeyPair(): KeyPair {

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA)

        generator.initialize(2048, SecureRandom())
        val keyPair = generator.genKeyPair()
        Timber.d("Taki klucz sie wygenerowal public ${keyPair.public.encoded.toBase64()}")
        Timber.d("Taki klucz sie wygenerowal private ${keyPair.private.encoded.toBase64()}")
        return keyPair
    }

    private fun getAESKeyBasedOnHash(localKey: String): SecretKey {
        val pbeKeySpec = PBEKeySpec(localKey.toCharArray(), ByteArray(100), 1024, 256)
        val aesKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(pbeKeySpec)
        return aesKey
    }

    fun encodeRSAKeys(rawKeys: KeyPair, localKey: String): Pair<String, String> {

        val aesKey = getAESKeyBasedOnHash(localKey)
        Timber.d("klucz sie wygenerowal: ${aesKey.encoded.toBase64()}")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val zeroBytes = ByteArray(cipher.blockSize)
        val ivParams = IvParameterSpec(zeroBytes)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivParams)
        val encodedPrivateKey = cipher.doFinal(rawKeys.private.encoded)
        val encodedPublicKey = cipher.doFinal(rawKeys.public.encoded)

        return Pair(encodedPublicKey.toBase64(), encodedPrivateKey.toBase64())
    }

    fun decodeRSAKeys(localKey: String, encodedKeys: Pair<String, String>): KeyPair {
        val aesKey = getAESKeyBasedOnHash(localKey)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        val zeroBytes = ByteArray(cipher.blockSize)
        val ivParams = IvParameterSpec(zeroBytes)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivParams)

        val decodedPublicKeyBytes = cipher.doFinal(encodedKeys.first.fromBase64())
        val decodedPrivateKeyBytes = cipher.doFinal(encodedKeys.second.fromBase64())

        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(decodedPublicKeyBytes))

        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(decodedPrivateKeyBytes))
        Timber.d("Taki klucz po odszyfrowaniu z dysku public ${publicKey.encoded.toBase64()}")
        Timber.d("Taki klucz po odszyfrowaniu z dysku  private ${privateKey.encoded.toBase64()}")
        return KeyPair(publicKey, privateKey)
    }

    fun getRSAKeysFromMemory(): Pair<String, String> {
        val privateKey = prefs.getString(PRIVATE_RSA_KEY, "") ?: ""
        val publicKey = prefs.getString(PUBLIC_RSA_KEY, "") ?: ""

        return Pair(publicKey, privateKey)
    }

    fun saveRSAKeysLocally(public: String, private: String) {
        with(prefs.edit()) {
            putString(pl.bsk.chatapp.PRIVATE_RSA_KEY, private)
            putString(pl.bsk.chatapp.PUBLIC_RSA_KEY, public)
            apply()
        }
    }


    fun generateSessionKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    fun encryptSessionKeyWithPublicKey(): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, partnerPublicKey)
        return cipher.doFinal(sessionKey.encoded).toBase64()
    }

    fun decryptSessionKeyWithPrivateKey(encryptedSessionKey: String) {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keyPairRSA.private)
        val decodedSessionKeyBytes = cipher.doFinal(encryptedSessionKey.fromBase64())
        sessionKey = SecretKeySpec(decodedSessionKeyBytes, 0, decodedSessionKeyBytes.size, "AES")

    }

    fun encryptMessage(encodingMode:String,objectToEncrypt: Serializable): ByteArray {
        val bytes = objectToEncrypt.serialize()
        val cipher = Cipher.getInstance("AES/" + encodingMode + "/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey)
        return cipher.doFinal(bytes)
    }

    fun decryptMessage(encodedType:String,content:ByteArray):Serializable{
        val cipher = Cipher.getInstance("AES/" + encodedType + "/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, sessionKey)

        return cipher.doFinal(content).deserialize()
    }


}