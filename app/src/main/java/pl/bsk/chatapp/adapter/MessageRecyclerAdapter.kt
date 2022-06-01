package pl.bsk.chatapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import pl.bsk.chatapp.R
import pl.bsk.chatapp.model.FileMessage
import pl.bsk.chatapp.model.Message

class MessageRecyclerAdapter(
    private var messageList: MutableList<Message>,
    private val onMessageClick: (Message) -> Unit,
) : RecyclerView.Adapter<MessageRecyclerAdapter.ViewHolder>() {

    fun getListSize() = messageList.size

    fun changeList(list: MutableList<Message>) {
        this.messageList = list
        notifyDataSetChanged()


    }

    fun addMessage(message: Message){
        this.messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.view_message_item, parent, false)
        return ViewHolder(viewHolder, onMessageClick)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messageList.get(position)
        holder.bind(message)
    }

    class ViewHolder(val v: View, private val onMessageClick: (Message) -> Unit,) : RecyclerView.ViewHolder(v) {

        fun bind(message: Message) {
            v.setOnClickListener{
                onMessageClick(message)
            }
            v.findViewById<TextView>(R.id.content_tv).text = message.content
            v.findViewById<TextView>(R.id.time_tv).text = message.sendTimeString()
            val container = v.findViewById<ConstraintLayout>(R.id.containter_cl)
            val param = container.layoutParams as ViewGroup.MarginLayoutParams
            if(message.isMine){
                param.setMargins(200,0,0,0)
            }
            else{
                param.setMargins(0,0,200,0)
            }
            container.layoutParams = param
        }
    }
}