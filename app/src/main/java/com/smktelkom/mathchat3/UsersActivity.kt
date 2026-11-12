package com.smktelkom.mathchat3

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.adapters.UsersAdapter
import com.smktelkom.mathchat3.databinding.ActivityUsersBinding
import com.smktelkom.mathchat3.databinding.DialogAddContactBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager
import java.util.UUID

class UsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var usersAdapter: UsersAdapter

    private var currentUserId: String = ""
    private var allOtherUsers: List<User> = emptyList()

    private var usersListener: ValueEventListener? = null
    private var favoritesListener: ValueEventListener? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        // Penanganan System Window Insets agar Header tidak tertutup Status Bar / Kamera
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        listenForData()
    }

    override fun onPause() {
        super.onPause()
        usersListener?.let { FirebaseHelper.removeUsersListener(it) }
        favoritesListener?.let { FirebaseHelper.removeFavoritesListener(currentUserId, it) }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(
            users = emptyList(),
            isFavorite = { user -> preferenceManager.isUserFavorite(user.id) },
            onFavoriteClicked = { user ->
                val isFavNow = preferenceManager.toggleUserFavorite(user.id)
                val message = if (isFavNow) {
                    "${user.name} ditambahkan ke Favorit ⭐"
                } else {
                    "${user.name} dihapus dari Favorit"
                }
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                filterAndDisplayUsers(binding.inputSearchContact.text.toString())
            },
            onUserClicked = { user ->
                val intent = Intent(applicationContext, ChatActivity::class.java)
                intent.putExtra("user", user)
                startActivity(intent)
                finish()
            }
        )
        binding.usersRecyclerView.adapter = usersAdapter
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonAddContact.setOnClickListener {
            showAddContactDialog()
        }

        // Real-time search filter berdasarkan nama, nomor HP, atau email
        binding.inputSearchContact.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplayUsers(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun listenForData() {
        loading(true)
        // 1. Sinkronisasi favorit realtime dari Firebase
        favoritesListener = FirebaseHelper.listenForFavorites(currentUserId, preferenceManager) {
            filterAndDisplayUsers(binding.inputSearchContact.text.toString())
        }

        // 2. Sinkronisasi pengguna realtime dari Firebase
        usersListener = FirebaseHelper.listenForUsers(this, preferenceManager) { allUsers ->
            loading(false)
            allOtherUsers = allUsers.filter { it.id != currentUserId }
            filterAndDisplayUsers(binding.inputSearchContact.text.toString())
        }
    }

    private fun filterAndDisplayUsers(query: String) {
        val q = query.trim().lowercase()
        val digitsQuery = q.replace("[^0-9]".toRegex(), "")

        val filtered = if (q.isEmpty()) {
            allOtherUsers
        } else {
            allOtherUsers.filter { user ->
                val nameMatch = user.name.lowercase().contains(q)
                val emailMatch = user.email.lowercase().contains(q)

                val rawPhone = user.phone.lowercase()
                val normalizedPhone = PreferenceManager.normalizePhone(user.phone).lowercase()
                val digitsPhone = user.phone.replace("[^0-9]".toRegex(), "")

                val phoneMatch = rawPhone.contains(q) ||
                        normalizedPhone.contains(q) ||
                        (digitsQuery.isNotEmpty() && digitsPhone.contains(digitsQuery))

                nameMatch || emailMatch || phoneMatch
            }
        }

        // Pengurutan: Kontak FAVORIT diletakkan di PALING ATAS, diikuti urutan abjad nama
        val sorted = filtered.sortedWith(
            compareByDescending<User> { preferenceManager.isUserFavorite(it.id) }
                .thenBy { it.name.lowercase() }
        )

        if (sorted.isEmpty()) {
            binding.usersRecyclerView.visibility = View.GONE
            binding.textErrorMessage.text = if (q.isEmpty()) {
                getString(R.string.no_other_users)
            } else {
                "Tidak ditemukan kontak untuk \"$query\""
            }
            binding.textErrorMessage.visibility = View.VISIBLE
        } else {
            binding.textErrorMessage.visibility = View.GONE
            binding.usersRecyclerView.visibility = View.VISIBLE
            usersAdapter.updateList(sorted)
        }
    }

    private fun showAddContactDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogAddContactBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialogBinding.buttonContactCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonContactSave.setOnClickListener {
            val phoneOrEmail = dialogBinding.inputContactPhoneOrEmail.text.toString().trim()
            val aliasName = dialogBinding.inputContactAlias.text.toString().trim()

            if (phoneOrEmail.isEmpty()) {
                Toast.makeText(this, "Masukkan Nomor HP atau Email teman!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val existing = preferenceManager.findUserByEmail(phoneOrEmail)
            if (existing != null && existing.id != currentUserId) {
                // User sudah terdaftar: otomatis tambahkan ke favorit & update nama jika ada alias baru
                val updated = if (aliasName.isNotEmpty()) existing.copy(name = aliasName) else existing
                preferenceManager.saveUser(updated)
                FirebaseHelper.saveUser(updated)

                if (!preferenceManager.isUserFavorite(updated.id)) {
                    preferenceManager.toggleUserFavorite(updated.id)
                }

                Toast.makeText(this, "${updated.name} berhasil disimpan dan ditambahkan ke Favorit ⭐", Toast.LENGTH_SHORT).show()
            } else {
                // Kontak baru
                val isEmail = phoneOrEmail.contains("@")
                val phone = if (isEmail) "" else PreferenceManager.normalizePhone(phoneOrEmail)
                val email = if (isEmail) phoneOrEmail else ""
                val displayName = aliasName.ifEmpty {
                    if (isEmail) phoneOrEmail.substringBefore("@") else phoneOrEmail
                }

                val newUser = User(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    email = email,
                    password = "contact_saved_default",
                    imageBase64 = AvatarGenerator.generateAvatarBase64(displayName),
                    phone = phone
                )

                preferenceManager.saveUser(newUser)
                FirebaseHelper.saveUser(newUser)
                preferenceManager.toggleUserFavorite(newUser.id)

                Toast.makeText(this, "Kontak '$displayName' berhasil disimpan ke Favorit ⭐", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            allOtherUsers = preferenceManager.getAllUsers().filter { it.id != currentUserId }
            filterAndDisplayUsers(binding.inputSearchContact.text.toString())
        }

        dialog.show()
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
}
