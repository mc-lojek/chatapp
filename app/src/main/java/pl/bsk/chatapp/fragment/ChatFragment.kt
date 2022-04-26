package pl.bsk.chatapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pl.bsk.chatapp.R
import pl.bsk.chatapp.adapter.MessageRecyclerAdapter
import pl.bsk.chatapp.model.Message
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import java.time.LocalTime

class ChatFragment : Fragment() {
    private lateinit var adapter: MessageRecyclerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val viewModel by activityViewModels<ClientServerViewModel>()

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
        setupAdapter()
    }

    private fun setupAdapter() {
        linearLayoutManager = LinearLayoutManager(this.activity)
        val recycler = requireActivity().findViewById<RecyclerView>(R.id.messages_rcl)
        recycler.layoutManager = linearLayoutManager

        adapter = MessageRecyclerAdapter(mutableListOf())
        recycler.adapter = adapter
    }

    private fun setupOnClicks() {
        requireActivity().findViewById<Button>(R.id.send_btn).setOnClickListener {
            val messageEditText = requireActivity().findViewById<EditText>(R.id.message_et)
            val content = messageEditText.text.toString()
            val recycler = requireActivity().findViewById<RecyclerView>(R.id.messages_rcl)
            val message = Message(LocalTime.now(), content, true)
            adapter.addMessage(message)
            recycler.smoothScrollToPosition(adapter.getListSize() - 1)
            messageEditText.text.clear()
            viewModel.sendMessageToServer(message)
        }
    }

}