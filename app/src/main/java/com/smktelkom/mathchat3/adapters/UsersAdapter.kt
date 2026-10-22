package com.smktelkom.mathchat3.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.databinding.ItemContainerUserBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator

class UsersAdapter(
    private val users: List<User>,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textName.text = user.name
            binding.textEmail.text = user.email

            if (user.imageBase64.isNotEmpty()) {
                val bitmap = AvatarGenerator.base64ToBitmap(user.imageBase64)
                if (bitmap != null) {
                    binding.imageProfile.setImageBitmap(bitmap)
                } else {
                    binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(user.name))
                }
            } else {
                binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(user.name))
            }

            binding.root.setOnClickListener {
                onUserClicked(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}
