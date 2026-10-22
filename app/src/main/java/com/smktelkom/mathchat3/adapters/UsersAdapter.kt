package com.smktelkom.mathchat3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.databinding.ItemContainerUserBinding
import com.smktelkom.mathchat3.models.SavedContact
import com.smktelkom.mathchat3.utils.AvatarGenerator

class UsersAdapter(
    private val contacts: List<SavedContact>,
    private val onContactClicked: (SavedContact) -> Unit,
    private val onFavoriteToggled: (SavedContact) -> Unit
) : RecyclerView.Adapter<UsersAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: SavedContact) {
            binding.textName.text = contact.customAliasName

            val detailsText = when {
                contact.phoneNumber.isNotEmpty() && contact.email.isNotEmpty() ->
                    "${contact.phoneNumber} • ${contact.email}"
                contact.phoneNumber.isNotEmpty() -> contact.phoneNumber
                else -> contact.email
            }
            binding.textDetails.text = detailsText

            if (contact.isFavorite) {
                binding.textFavoriteBadge.visibility = View.VISIBLE
                binding.imageFavoriteStar.setImageResource(R.drawable.ic_star_filled)
            } else {
                binding.textFavoriteBadge.visibility = View.GONE
                binding.imageFavoriteStar.setImageResource(R.drawable.ic_star_outline)
            }

            if (contact.imageBase64.isNotEmpty()) {
                val bitmap = AvatarGenerator.base64ToBitmap(contact.imageBase64)
                if (bitmap != null) {
                    binding.imageProfile.setImageBitmap(bitmap)
                } else {
                    binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(contact.customAliasName))
                }
            } else {
                binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(contact.customAliasName))
            }

            binding.imageFavoriteStar.setOnClickListener {
                onFavoriteToggled(contact)
            }

            binding.root.setOnClickListener {
                onContactClicked(contact)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size
}
