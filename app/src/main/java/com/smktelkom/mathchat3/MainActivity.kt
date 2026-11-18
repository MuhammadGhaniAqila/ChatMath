package com.smktelkom.mathchat3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.adapters.RecentConversationsAdapter
import com.smktelkom.mathchat3.databinding.ActivityMainBinding
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.DialogHelper
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.FormulaManager
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.NotificationHelper
import com.smktelkom.mathchat3.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private var chatsListener: ValueEventListener? = null
    private var currentUserId: String = ""
    private var allRecentConversations: List<ChatMessage> = emptyList()
    private var allUsersCache: List<User> = emptyList()
    private var unreadCountsMap: Map<String, Int> = emptyMap()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationHelper.createNotificationChannel(this)
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak. Notifikasi pesan tidak akan muncul.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        // Penanganan System Window Insets agar Header maroon menyatu rapi dengan Status Bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutTopBar.setPadding(0, systemBars.top, 0, 0)
            binding.root.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Mode 1: Otomatis acak rumus jika aplikasi baru saja dibuka dari keadaan tertutup
        FormulaManager.checkAndInitializeSessionFormula(this)

        checkNotificationPermission()
        loadUserDetails()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserDetails()
        refreshLocalConversations()
        listenForRecentConversations()
    }

    override fun onPause() {
        super.onPause()
        chatsListener?.let { FirebaseHelper.removeChatsListener(it) }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationHelper.createNotificationChannel(this)
            }
        } else {
            NotificationHelper.createNotificationChannel(this)
        }
    }

    private fun loadUserDetails() {
        val name = preferenceManager.getString(PreferenceManager.KEY_NAME) ?: "Pengguna"
        binding.textName.text = name

        val imageBase64 = preferenceManager.getString(PreferenceManager.KEY_IMAGE) ?: ""
        if (imageBase64.isNotEmpty()) {
            val bitmap = AvatarGenerator.base64ToBitmap(imageBase64)
            if (bitmap != null) {
                binding.imageProfile.setImageBitmap(bitmap)
            } else {
                binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(name))
            }
        } else {
            binding.imageProfile.setImageBitmap(AvatarGenerator.generateAvatarBitmap(name))
        }
    }

    private fun setListeners() {
        binding.imageProfile.setOnClickListener {
            startActivity(Intent(applicationContext, ProfileActivity::class.java))
        }

        binding.tabContacts.setOnClickListener {
            startActivity(Intent(applicationContext, UsersActivity::class.java))
        }

        binding.tabFormula.setOnClickListener {
            showFormulaConfigDialog()
        }

        binding.imageSearch.setOnClickListener {
            if (binding.layoutSearchBar.visibility == View.VISIBLE) {
                closeSearchBar()
            } else {
                binding.layoutSearchBar.visibility = View.VISIBLE
                binding.inputSearchChats.requestFocus()
            }
        }

        binding.buttonCloseSearch.setOnClickListener {
            closeSearchBar()
        }

        binding.inputSearchChats.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                displayFilteredConversations(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.imageMore.setOnClickListener { view ->
            showPopupMenu(view)
        }

        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(applicationContext, UsersActivity::class.java))
        }
    }

    private fun closeSearchBar() {
        binding.inputSearchChats.text.clear()
        binding.layoutSearchBar.visibility = View.GONE
        displayFilteredConversations("")
    }

    private fun showFormulaConfigDialog() {
        DialogHelper.showCustomFormulaDialog(this) {
            Toast.makeText(this, FormulaManager.getFormulaBannerText(this), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPopupMenu(anchorView: View) {
        DialogHelper.showCustomPopupMenu(
            this,
            anchorView,
            onFormulaClicked = {
                showFormulaConfigDialog()
            },
            onProfileClicked = {
                startActivity(Intent(applicationContext, ProfileActivity::class.java))
            },
            onLanguageClicked = {
                showLanguageSelectionDialog()
            }
        )
    }

    private fun showLanguageSelectionDialog() {
        val currentLang = LocaleHelper.getSelectedLanguage(this)
        DialogHelper.showLanguageDialog(this, currentLang) { targetLang ->
            if (targetLang != currentLang) {
                LocaleHelper.setLocale(this, targetLang)
                recreate()
            }
        }
    }

    private fun refreshLocalConversations() {
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""
        allRecentConversations = preferenceManager.getRecentConversations(currentUserId)
        allUsersCache = preferenceManager.getAllUsers()
        unreadCountsMap = preferenceManager.getUnreadCountsMap(currentUserId)

        updateChatTabBadge()
        displayFilteredConversations(binding.inputSearchChats.text.toString())
    }

    private fun updateChatTabBadge() {
        val unreadChatsCount = unreadCountsMap.size
        if (unreadChatsCount > 0) {
            binding.textChatCount.text = if (unreadChatsCount > 99) "99+" else unreadChatsCount.toString()
            binding.textChatCount.visibility = View.VISIBLE
        } else {
            binding.textChatCount.visibility = View.GONE
        }
    }

    private fun listenForRecentConversations() {
        if (allRecentConversations.isEmpty()) {
            binding.progressBar.visibility = View.VISIBLE
        }
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        chatsListener = FirebaseHelper.listenForRecentConversations(
            this,
            currentUserId,
            preferenceManager
        ) { recentConversations ->
            binding.progressBar.visibility = View.GONE
            allRecentConversations = recentConversations
            allUsersCache = preferenceManager.getAllUsers()
            unreadCountsMap = preferenceManager.getUnreadCountsMap(currentUserId)

            updateChatTabBadge()
            displayFilteredConversations(binding.inputSearchChats.text.toString())
        }
    }

    private fun displayFilteredConversations(query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            allRecentConversations
        } else {
            allRecentConversations.filter { msg ->
                val otherName = if (msg.senderId == currentUserId) msg.receiverName else msg.senderName
                otherName.lowercase().contains(q) || msg.plaintext.lowercase().contains(q)
            }
        }

        if (filtered.isNotEmpty()) {
            val adapter = RecentConversationsAdapter(
                filtered,
                currentUserId,
                allUsersCache,
                unreadCountsMap
            ) { selectedUser ->
                val intent = Intent(applicationContext, ChatActivity::class.java)
                intent.putExtra("user", selectedUser)
                startActivity(intent)
            }
            binding.conversationsRecyclerView.adapter = adapter
            binding.conversationsRecyclerView.visibility = View.VISIBLE
            binding.textNoConversations.visibility = View.GONE
        } else {
            binding.conversationsRecyclerView.visibility = View.GONE
            binding.textNoConversations.text = if (q.isEmpty()) {
                getString(R.string.no_conversations)
            } else {
                "Tidak ditemukan percakapan untuk \"$query\""
            }
            binding.textNoConversations.visibility = View.VISIBLE
        }
    }
}
