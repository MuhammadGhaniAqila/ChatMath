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
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var preferenceManager: PreferenceManager
    private var usersListener: ValueEventListener? = null

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
        listenForUsers()
    }

    override fun onPause() {
        super.onPause()
        usersListener?.let { FirebaseHelper.removeUsersListener(it) }
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun listenForUsers() {
        loading(true)
        val currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        usersListener = FirebaseHelper.listenForUsers(this, preferenceManager) { allUsers ->
            loading(false)
            val otherUsers = allUsers.filter { it.id != currentUserId }
            if (otherUsers.isNotEmpty()) {
                val usersAdapter = UsersAdapter(otherUsers) { user ->
                    val intent = Intent(applicationContext, ChatActivity::class.java)
                    intent.putExtra("user", user)
                    startActivity(intent)
                    finish()
                }
                binding.usersRecyclerView.adapter = usersAdapter
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
