package com.smktelkom.mathchat3.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.databinding.ItemContainerRecentConversionBinding
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator

class RecentConversationsAdapter(
    private val chatMessages: List<ChatMessage>,
    private val currentUserId: String,
    private val allUsers: List<User>,
    private val onConversationClicked: (User) -> Unit
) : RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder>() {

    inner class ConversionViewHolder(private val binding: ItemContainerRecentConversionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
            val otherUserName = if (message.senderId == currentUserId) message.receiverName else message.senderName
            val otherUser = allUsers.firstOrNull { it.id == otherUserId }

            binding.textName.text = otherUserName
            binding.textRecentMessage.text = message.plaintext
            binding.textDateTime.text = message.dateTime

            val imageBase64 = otherUser?.imageBase64 ?: ""
            if (imageBase64.isNotEmpty()) {
                val bitmap = AvatarGenerator.base64ToBitmap(imageBase64)
                if (bitmap != null) {
                    binding.imageProfile.setImageBitmap(bitmap)
                } else {
                    binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(otherUserName))
                }
            } else {
                binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(otherUserName))
            }

            binding.root.setOnClickListener {
                val userToPass = otherUser ?: User(id = otherUserId, name = otherUserName)
                onConversationClicked(userToPass)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        val binding = ItemContainerRecentConversionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        holder.bind(chatMessages[position])
    }

    override fun getItemCount(): Int = chatMessages.size
}
