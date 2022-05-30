package pl.bsk.chatapp.fragment

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.bsk.chatapp.FILE_CHOOSE_REQUEST_CODE
import pl.bsk.chatapp.R
import pl.bsk.chatapp.adapter.MessageRecyclerAdapter
import pl.bsk.chatapp.model.Message
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import timber.log.Timber
import java.io.File
import java.time.LocalTime


class ChatFragment : Fragment() {
    private lateinit var adapter: MessageRecyclerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val viewModel by activityViewModels<ClientServerViewModel>()

    private lateinit var recycler: RecyclerView

    private val observer = Observer<Message?> { newMessage ->



        if (newMessage != null) {
            adapter.addMessage(newMessage)
            recycler.smoothScrollToPosition(adapter.getListSize() - 1)
            Timber.d("wiadomosc obserwuje ${newMessage}")
        } else {
            Timber.d("null tu jest w obserwerze")
        }

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
        setupAdapter()
        setupOnClicks()

        viewModel.newMessageLiveData.observe(viewLifecycleOwner, observer)
    }

    private fun setupAdapter() {
        recycler = requireActivity().findViewById(R.id.messages_rcl)

        linearLayoutManager = LinearLayoutManager(this.activity)
        recycler.layoutManager = linearLayoutManager
        adapter = MessageRecyclerAdapter(mutableListOf())
        recycler.adapter = adapter
    }

    private fun setupOnClicks() {

        requireActivity().findViewById<Button>(R.id.send_btn).setOnClickListener {
            val messageEditText = requireActivity().findViewById<EditText>(R.id.message_et)
            val content = messageEditText.text.toString()
            val message = Message(LocalTime.now(), content, true)
            messageEditText.text.clear()
            viewModel.sendMessageToServer(message)
        }

        requireActivity().findViewById<Button>(R.id.send_file_btn).setOnClickListener {

            val file = File("/sdcard/Download/Untitled2.jpg")

            pickFileToSend()

            //viewModel.sendFile(file)
        }
    }

    private fun pickFileToSend() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(intent, FILE_CHOOSE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == FILE_CHOOSE_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedFile = data?.data

            viewModel.sendFile(selectedFile!!, requireContext())




//
//            if(selectedFile != null) {
//                val file = File(selectedFile)
//                Timber.d("taki rozmiarek i nzawa pliku ${file.length()} ${file.name}")
//
//                viewModel.sendFile(file)
//            } else {
//                Timber.d("dupa dupa dupa null")
//            }
        }
    }
}