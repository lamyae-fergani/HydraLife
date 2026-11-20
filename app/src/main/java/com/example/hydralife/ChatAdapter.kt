package com.example.hydralife

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_TYPING = 3
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
    }

    class AiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val layoutCopy: View = itemView.findViewById(R.id.layoutCopy)
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTyping: TextView = itemView.findViewById(R.id.tvTyping)
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.isLoadingBubble -> VIEW_TYPE_TYPING
            msg.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_TYPING -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_typing, parent, false)
                TypingViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_ai, parent, false)
                AiViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserViewHolder -> {
                holder.tvMessage.text = message.text
            }
            is AiViewHolder -> {
                holder.tvMessage.text = formatMarkdownToHtml(message.text)
                holder.layoutCopy.setOnClickListener {
                    val context = holder.itemView.context
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("HydraLife AI Response", message.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }
            }
            is TypingViewHolder -> {
                holder.tvTyping.text = "HydraLife AI is thinking..."
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    private fun formatMarkdownToHtml(markdown: String): android.text.Spanned {
        val text = markdown
            .replace(Regex("^### (.*)$", RegexOption.MULTILINE), "<b><h3>$1</h3></b>")
            .replace(Regex("^## (.*)$", RegexOption.MULTILINE), "<b><h2>$1</h2></b>")
            .replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<b><h1>$1</h1></b>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
            .replace(Regex("^[-*] (.*)$", RegexOption.MULTILINE), "• $1")
            .replace("\n", "<br>")

        return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
    }
}
