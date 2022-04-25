package pl.bsk.chatapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.bsk.chatapp.R
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import timber.log.Timber
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.PrintWriter

class ChatFragment : Fragment() {

    private val viewModel by activityViewModels<ClientServerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOnClicks()
    }

    private fun setupOnClicks() {
        requireActivity().findViewById<Button>(R.id.send_btn).setOnClickListener {

            val message = requireActivity().findViewById<EditText>(R.id.message_et).text.toString()
            viewModel.sendMessageToServer(message)
        }
    }

}