package pl.bsk.chatapp.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.bsk.chatapp.FILE_CHOOSE_REQUEST_CODE
import pl.bsk.chatapp.MY_MESSAGE_INDEX_COUNTER
import pl.bsk.chatapp.R
import pl.bsk.chatapp.adapter.MessageRecyclerAdapter
import pl.bsk.chatapp.model.FileMessage
import pl.bsk.chatapp.model.FileSendProgress
import pl.bsk.chatapp.model.Message
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import timber.log.Timber
import java.time.LocalTime


class ChatFragment : Fragment() {
    private lateinit var adapter: MessageRecyclerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val viewModel by activityViewModels<ClientServerViewModel>()

    private lateinit var recycler: RecyclerView
    private lateinit var progressContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var progressStateText: TextView

    private val observer = Observer<Message?> { newMessage ->
        if (newMessage != null) {

            if (newMessage is FileMessage) {
                Timber.d("odebralem pliczek ${newMessage.uri.path}")
                Timber.d(getMimeType(newMessage.uri.path))
            }

            viewModel.messagesList.add(newMessage)
            adapter.notifyItemInserted(viewModel.messagesList.size - 1)
            recycler.smoothScrollToPosition(adapter.getListSize() - 1)
            Timber.d("wiadomosc obserwuje ${newMessage}")
        } else {
            Timber.d("null tu jest w obserwerze")
        }
    }

    private val responseObserver = Observer<Int?> { confirmedId ->
        if (confirmedId != null) {

            viewModel.messagesList.last { it.isMine && it.id == confirmedId }.isRead = true
            adapter.notifyDataSetChanged()
            //Todo to jest do zmiany bo kosztowne

        } else {
            Timber.d("null tu jest w obserwerze")
        }
    }

    private val fileObserver = Observer<FileSendProgress?> { progress ->
        if (progress != null) {

            progressBar.progress = progress.progress
            if (progress.errorMessage == null) {
                progressStateText.text = "${progress.progress}%"
            } else {
                progressStateText.text = progress.errorMessage
            }
            progressContainer.visibility = View.VISIBLE

        } else {
            progressBar.progress = 0
            progressStateText.text = ""
            progressContainer.visibility = View.GONE
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
        setupViews()

        viewModel.newMessageLiveData.observe(viewLifecycleOwner, observer)
        viewModel.confirmationResponseLiveData.observe(viewLifecycleOwner, responseObserver)
        viewModel.fileSendingStatusLiveData.observe(viewLifecycleOwner, fileObserver)
    }

    private fun setupAdapter() {
        recycler = requireActivity().findViewById(R.id.messages_rcl)

        linearLayoutManager = LinearLayoutManager(this.activity)
        recycler.layoutManager = linearLayoutManager
        adapter = MessageRecyclerAdapter(viewModel.messagesList) {
            if (it is FileMessage) {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(it.uri, getMimeType(it.uri.path))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if(checkAvailableApps(intent))
                    startActivity(intent)
            }
        }
        recycler.adapter = adapter
    }

    private fun setupOnClicks() {

        requireActivity().findViewById<Button>(R.id.send_btn).setOnClickListener {
            val messageEditText = requireActivity().findViewById<EditText>(R.id.message_et)
            val content = messageEditText.text.toString()
            val message = Message(LocalTime.now(), content, true, MY_MESSAGE_INDEX_COUNTER)
            MY_MESSAGE_INDEX_COUNTER++
            messageEditText.text.clear()
            viewModel.sendMessageToServer(message)
        }

        requireActivity().findViewById<Button>(R.id.send_file_btn).setOnClickListener {
            pickFileToSend()
        }
    }

    private fun setupViews() {
        progressContainer = requireActivity().findViewById(R.id.pb_container_ll)
        progressBar = requireActivity().findViewById(R.id.progress_bar)
        progressStateText = requireActivity().findViewById(R.id.pb_state_tv)
    }

    private fun pickFileToSend() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(intent, FILE_CHOOSE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSE_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedFile = data?.data

            viewModel.sendFile(selectedFile!!, requireContext())
        }
    }

    private fun checkAvailableApps(intent: Intent): Boolean {
        val manager: PackageManager = requireActivity().packageManager
        val info = manager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        Timber.d("znaleziono tyle apek ${info.size}")
        if (info.isEmpty()){
            Toast.makeText(
                requireContext(),
                "No app found to open this file",
                Toast.LENGTH_SHORT
            ).show()
            return false
        } else return true
    }

    private fun getMimeType(uri: String?): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }
}