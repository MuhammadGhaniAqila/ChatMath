package com.smktelkom.mathchat3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.adapters.UsersAdapter
import com.smktelkom.mathchat3.databinding.ActivityUsersBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.DialogHelper
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var preferenceManager: PreferenceManager
    private var contactsListener: ValueEventListener? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)

        // Penanganan System Window Insets agar Header tidak tertutup Status Bar / Kamera
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setListeners()
    }

    override fun onResume() {
        super.onResume()
        listenForSavedContacts()
    }

    override fun onPause() {
        super.onPause()
        val currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""
        contactsListener?.let { FirebaseHelper.removeContactsListener(currentUserId, it) }
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonAddContact.setOnClickListener {
            DialogHelper.showAddContactDialog(this, preferenceManager) {
                listenForSavedContacts()
            }
        }
    }

    private fun listenForSavedContacts() {
        loading(true)
        val currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        contactsListener = FirebaseHelper.listenForSavedContacts(
            this,
            currentUserId,
            preferenceManager
        ) { savedContacts ->
            loading(false)
            if (savedContacts.isNotEmpty()) {
                val adapter = UsersAdapter(
                    savedContacts,
                    onContactClicked = { contact ->
                        val friendUser = preferenceManager.getAllUsers().firstOrNull { it.id == contact.contactUserId }
                            ?: User(
                                id = contact.contactUserId,
                                name = contact.customAliasName,
                                phoneNumber = contact.phoneNumber,
                                email = contact.email,
                                imageBase64 = contact.imageBase64
                            )
                        val intent = Intent(applicationContext, ChatActivity::class.java)
                        intent.putExtra("user", friendUser)
                        startActivity(intent)
                        finish()
                    },
                    onFavoriteToggled = { contact ->
                        val isFav = preferenceManager.toggleContactFavorite(currentUserId, contact.contactUserId)
                        val updated = contact.copy(isFavorite = isFav)
                        FirebaseHelper.saveContact(currentUserId, updated)
                        listenForSavedContacts() // Reload list order
                    }
                )
                binding.usersRecyclerView.adapter = adapter
                binding.usersRecyclerView.visibility = View.VISIBLE
                binding.textErrorMessage.visibility = View.GONE
            } else {
                binding.usersRecyclerView.visibility = View.GONE
                binding.textErrorMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
}
