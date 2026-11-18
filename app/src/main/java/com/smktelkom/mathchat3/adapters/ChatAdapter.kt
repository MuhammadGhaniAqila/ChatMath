package com.smktelkom.mathchat3.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.databinding.ItemContainerReceivedMessageBinding
import com.smktelkom.mathchat3.databinding.ItemContainerSentMessageBinding
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.FormulaManager

class ChatAdapter(
    private val chatMessages: List<ChatMessage>,
    private val receiverProfileImage: Bitmap?,
    private val senderId: String,
    private val onMessageClicked: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    inner class SentMessageViewHolder(private val binding: ItemContainerSentMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.textMessage.text = message.plaintext
            val fStr = FormulaManager.formatLinear(message.coeffA, message.coeffB)
            val gStr = FormulaManager.formatLinear(message.coeffC, message.coeffD)
            val fogStr = FormulaManager.formatLinear(message.coeffA * message.coeffC, (message.coeffA * message.coeffD) + message.coeffB)
            binding.textFormulaUsed.text = "Rumus: f(x)=$fStr, g(x)=$gStr ➔ $fogStr"
            binding.textCipherPreview.text = "Cipher: ${message.ciphertext}"
            binding.textDateTime.text = message.dateTime

            binding.layoutMessageBubble.setOnClickListener {
                onMessageClicked(message)
            }
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.textMessage.text = message.plaintext
            val fStr = FormulaManager.formatLinear(message.coeffA, message.coeffB)
            val gStr = FormulaManager.formatLinear(message.coeffC, message.coeffD)
            val fogStr = FormulaManager.formatLinear(message.coeffA * message.coeffC, (message.coeffA * message.coeffD) + message.coeffB)
            binding.textFormulaUsed.text = "Rumus: f(x)=$fStr, g(x)=$gStr ➔ $fogStr"
            binding.textCipherPreview.text = "Cipher: ${message.ciphertext}"
            binding.textDateTime.text = message.dateTime

            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage)
            } else {
                binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(message.senderName))
            }

            binding.layoutMessageBubble.setOnClickListener {
                onMessageClicked(message)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].senderId == senderId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemContainerSentMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            SentMessageViewHolder(binding)
        } else {
            val binding = ItemContainerReceivedMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ReceivedMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(chatMessages[position])
        } else {
            (holder as ReceivedMessageViewHolder).bind(chatMessages[position])
        }
    }

    override fun getItemCount(): Int = chatMessages.size
}
