package com.smktelkom.mathchat3.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.databinding.ItemContainerRecentConversionBinding
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator

class RecentConversationsAdapter(
    private val chatMessages: List<ChatMessage>,
    private val currentUserId: String,
    private val allUsers: List<User>,
    private val unreadCounts: Map<String, Int> = emptyMap(),
    private val onConversationClicked: (User) -> Unit
) : RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder>() {

    inner class ConversionViewHolder(private val binding: ItemContainerRecentConversionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val context = binding.root.context
            val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
            val otherUserName = if (message.senderId == currentUserId) message.receiverName else message.senderName
            val otherUser = allUsers.firstOrNull { it.id == otherUserId }

            binding.textName.text = otherUserName
            binding.textRecentMessage.text = message.plaintext
            binding.textTime.text = message.dateTime

            val unreadCount = unreadCounts[otherUserId] ?: 0
            if (unreadCount > 0) {
                binding.textUnreadCount.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                binding.textUnreadCount.visibility = View.VISIBLE
                binding.textTime.setTextColor(ContextCompat.getColor(context, R.color.primary))
                binding.textTime.setTypeface(null, Typeface.BOLD)
                binding.textRecentMessage.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
                binding.textRecentMessage.setTypeface(null, Typeface.BOLD)
            } else {
                binding.textUnreadCount.visibility = View.GONE
                binding.textTime.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                binding.textTime.setTypeface(null, Typeface.NORMAL)
                binding.textRecentMessage.setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
                binding.textRecentMessage.setTypeface(null, Typeface.NORMAL)
            }

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
