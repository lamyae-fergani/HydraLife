package com.example.hydralife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hydralife.data.ConversationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationsAdapter(
    private val conversations: List<ConversationEntity>,
    private val onConvClick: (ConversationEntity) -> Unit,
    private val onRenameClick: (ConversationEntity) -> Unit,
    private val onDeleteClick: (ConversationEntity) -> Unit
) : RecyclerView.Adapter<ConversationsAdapter.ConvViewHolder>() {

    class ConvViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvConvTitle: TextView = itemView.findViewById(R.id.tvConvTitle)
        val tvConvDate: TextView = itemView.findViewById(R.id.tvConvDate)
        val btnRenameConv: ImageButton = itemView.findViewById(R.id.btnRenameConv)
        val btnDeleteConv: ImageButton = itemView.findViewById(R.id.btnDeleteConv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConvViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConvViewHolder, position: Int) {
        val conv = conversations[position]
        holder.tvConvTitle.text = conv.title
        val dateStr = SimpleDateFormat("MMM d, yyyy - HH:mm", Locale.getDefault()).format(Date(conv.updatedAt))
        holder.tvConvDate.text = dateStr

        holder.itemView.setOnClickListener { onConvClick(conv) }
        holder.btnRenameConv.setOnClickListener { onRenameClick(conv) }
        holder.btnDeleteConv.setOnClickListener { onDeleteClick(conv) }
    }

    override fun getItemCount(): Int = conversations.size
}
