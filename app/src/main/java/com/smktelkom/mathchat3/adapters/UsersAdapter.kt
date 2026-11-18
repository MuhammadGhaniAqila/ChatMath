package com.smktelkom.mathchat3.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.databinding.ItemContainerUserBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator

class UsersAdapter(
    private var users: List<User>,
    private val isFavorite: (User) -> Boolean,
    private val onFavoriteClicked: (User) -> Unit,
    private val onUserClicked: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textName.text = user.name
            binding.textPhoneOrEmail.text = when {
                user.email.isNotEmpty() && user.phone.isNotEmpty() -> "${user.email} • ${user.phone}"
                user.email.isNotEmpty() -> user.email
                user.phone.isNotEmpty() -> user.phone
                else -> "-"
            }

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

            val fav = isFavorite(user)
            if (fav) {
                binding.imageFavoriteStar.setImageResource(R.drawable.ic_star_filled)
                binding.imageFavoriteStar.contentDescription = "Hapus dari Favorit"
            } else {
                binding.imageFavoriteStar.setImageResource(R.drawable.ic_star_outline)
                binding.imageFavoriteStar.contentDescription = "Tambah ke Favorit"
            }

            // Klik bintang HANYA toggle favorit, tidak membuka chat
            binding.imageFavoriteStar.setOnClickListener {
                onFavoriteClicked(user)
            }

            binding.root.setOnClickListener {
                onUserClicked(user)
            }
        }
    }

    fun updateList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
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
