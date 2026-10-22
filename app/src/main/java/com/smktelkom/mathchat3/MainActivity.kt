package com.smktelkom.mathchat3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.adapters.RecentConversationsAdapter
import com.smktelkom.mathchat3.databinding.ActivityMainBinding
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

        // Penanganan System Window Insets agar Header tidak tertutup Status Bar / Kamera
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // Mode 1: Otomatis acak rumus jika aplikasi baru saja dibuka dari keadaan tertutup
        FormulaManager.checkAndInitializeSessionFormula(this)

        checkNotificationPermission()
        loadUserDetails()
        setListeners()
        updateFormulaBanner()
    }

    override fun onResume() {
        super.onResume()
        loadUserDetails()
        updateFormulaBanner()
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

    private fun updateFormulaBanner() {
        binding.textFormulaBanner.text = FormulaManager.getFormulaBannerText(this) + "  [Ubah Rumus]"
    }

    private fun setListeners() {
        binding.imageFormula.setOnClickListener {
            showFormulaConfigDialog()
        }

        binding.textFormulaBanner.setOnClickListener {
            showFormulaConfigDialog()
        }

        binding.imageMore.setOnClickListener { view ->
            showPopupMenu(view)
        }

        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(applicationContext, UsersActivity::class.java))
        }
    }

    private fun showFormulaConfigDialog() {
        DialogHelper.showCustomFormulaDialog(this) {
            updateFormulaBanner()
        }
    }

    private fun showPopupMenu(anchorView: View) {
        DialogHelper.showCustomPopupMenu(
            this,
            anchorView,
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
                recreate() // Reload activity UI to apply new strings
            }
        }
    }

    private fun listenForRecentConversations() {
        binding.progressBar.visibility = View.VISIBLE
        val currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""

        chatsListener = FirebaseHelper.listenForRecentConversations(
            this,
            currentUserId,
            preferenceManager
        ) { recentConversations ->
            binding.progressBar.visibility = View.GONE
            val allUsers = preferenceManager.getAllUsers()

            if (recentConversations.isNotEmpty()) {
                val adapter = RecentConversationsAdapter(
                    recentConversations,
                    currentUserId,
                    allUsers
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
                binding.textNoConversations.visibility = View.VISIBLE
            }
        }
    }
}
