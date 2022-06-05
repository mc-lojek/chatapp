package pl.bsk.chatapp.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyProperties
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import pl.bsk.chatapp.*
import sun.security.krb5.Confounder.bytes
import timber.log.Timber
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


class LoginFragment : Fragment() {

    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)

        setupOnClicks()
    }

    private fun setupOnClicks() {
        requireActivity().findViewById<Button>(R.id.login_btn).setOnClickListener {

            val password =
                requireActivity().findViewById<EditText>(R.id.password_et).text.toString()
            login(password)



        }
    }

    private fun login(password: String) {
        val storedHash = prefs.getString(PASSWORD_HASH_KEY, "") ?: ""
        val insertedHash = hashPassword(password)

        Timber.d("podane z klawiatury: ${insertedHash}koniec")
        Timber.d("zapisane w prefsach: ${storedHash}koniec")

        if (storedHash.equals("")) {
            register(insertedHash)
        } else if (storedHash.trim().equals(insertedHash.trim())) {
            Timber.d("haslo poprawne, chodzmy dalej")
        } else {
            //Toast.makeText(requireContext(), "Podales zle haslo!", Toast.LENGTH_LONG).show()
        }

        encodeRSAKeys(generateRSAKeyPair(), insertedHash)

    }

    private fun register(hash: String) {
        with(prefs.edit()) {
            putString(PASSWORD_HASH_KEY, hash)
            apply()
        }
    }

    private fun hashPassword(password: String): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val encodedhash: ByteArray = digest.digest(
            password.encodeToByteArray()
        )

        Timber.d("takie jest haslo do zaszyfrowania: ${password}")
        Timber.d("taka jest byte array po zaszyfrowaniu: ${encodedhash.toBase64()}")


        return encodedhash.toBase64()
    }

    private fun generateRSAKeyPair(): KeyPair {

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

    private fun encodeRSAKeys(rawKeys: KeyPair, localKey: String): Pair<String, String> {

        val aesKey = getAESKeyBasedOnHash(localKey)
        Timber.d("klucz sie wygenerowal: ${aesKey.encoded.toBase64()}")

        val cipher = Cipher.getInstance("AES/CBC/NOPADDING")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val encodedPrivateKey = cipher.doFinal(rawKeys.private.encoded)
        val encodedPublicKey = cipher.doFinal(rawKeys.public.encoded)

        return Pair(encodedPublicKey.toBase64(), encodedPrivateKey.toBase64())
    }

    private fun decodeRSAKeys(localKey: String): KeyPair {
        val encodedKeys = getRSAKeysFromMemory()
        val aesKey = getAESKeyBasedOnHash(localKey)

        val cipher = Cipher.getInstance("AES/CBC/NOPADDING")
        cipher.init(Cipher.DECRYPT_MODE, aesKey)

        val decodedPublicKeyBytes = cipher.doFinal(encodedKeys.first.fromBase64())
        val decodedPrivateKeyBytes = cipher.doFinal(encodedKeys.second.fromBase64())

        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(decodedPublicKeyBytes))

        TODO()

        //return KeyPair(Priva, decodedPrivateKey)
    }

    private fun getRSAKeysFromMemory(): Pair<String, String> {
        val privateKey = prefs.getString(PRIVATE_RSA_KEY, "") ?: ""
        val publicKey = prefs.getString(PUBLIC_RSA_KEY, "") ?: ""

        return Pair(publicKey, privateKey)
    }

    private fun saveRSAKeysLocally(private: String, public: String) {
        with(prefs.edit()) {
            putString(PRIVATE_RSA_KEY, private)
            putString(PUBLIC_RSA_KEY, public)
        }
    }

}