package pl.bsk.chatapp.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pl.bsk.chatapp.*
import timber.log.Timber
import java.security.*


class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        val loginSucceded=CryptoManager.login(password)
        findNavController().navigate(R.id.action_loginFragment_to_connectFragment)
    }



}