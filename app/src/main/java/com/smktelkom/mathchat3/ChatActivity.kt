package com.smktelkom.mathchat3

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.adapters.ChatAdapter
import com.smktelkom.mathchat3.databinding.ActivityChatBinding
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.CryptoHelper
import com.smktelkom.mathchat3.utils.DialogHelper
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.FormulaManager
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var receiverUser: User
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private var receiverProfileBitmap: Bitmap? = null
    private var chatsListener: ValueEventListener? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)

        // Penanganan System Window Insets & Soft Keyboard (IME)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            binding.root.setPadding(0, systemBars.top, 0, bottomPadding)
            insets
        }

        loadReceiverDetails()
        setListeners()
        updateFormulaBanner()
    }

    override fun onResume() {
        super.onResume()
        if (::receiverUser.isInitialized) {
            FirebaseHelper.currentActiveChatReceiverId = receiverUser.id
        }
        updateFormulaBanner()
        listenForMessages()
    }

    override fun onPause() {
        super.onPause()
        FirebaseHelper.currentActiveChatReceiverId = null
        chatsListener?.let { FirebaseHelper.removeChatsListener(it) }
    }

    private fun loadReceiverDetails() {
        @Suppress("DEPRECATION")
        receiverUser = (intent.getSerializableExtra("user") as? User) ?: return
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""
        currentUserName = preferenceManager.getString(PreferenceManager.KEY_NAME) ?: "Pengguna"

        binding.textName.text = receiverUser.name

        if (receiverUser.imageBase64.isNotEmpty()) {
            receiverProfileBitmap = AvatarGenerator.base64ToBitmap(receiverUser.imageBase64)
        }
        if (receiverProfileBitmap == null) {
            receiverProfileBitmap = AvatarGenerator.generateAvatarBitmap(receiverUser.name)
        }
        binding.imageProfile.setImageBitmap(receiverProfileBitmap)
    }

    private fun updateFormulaBanner() {
        binding.textFormulaBanner.text = FormulaManager.getFormulaBannerText(this) + "  [Ubah Rumus]"
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            finish()
        }

        binding.buttonExample.setOnClickListener {
            // Tombol pintasan untuk mengisi otomatis "Telkom Purwokerto"
            binding.inputMessage.setText("Telkom Purwokerto")
            binding.inputMessage.setSelection(binding.inputMessage.text.length)
        }

        binding.imageFormula.setOnClickListener {
            showFormulaConfigDialog()
        }

        binding.textFormulaBanner.setOnClickListener {
            showFormulaConfigDialog()
        }

        binding.layoutSend.setOnClickListener {
            sendMessage()
        }

        binding.inputMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun showFormulaConfigDialog() {
        DialogHelper.showCustomFormulaDialog(this) {
            updateFormulaBanner()
        }
    }

    private fun sendMessage() {
        val plaintext = binding.inputMessage.text.toString().trim()
        if (plaintext.isEmpty()) return

        // Validasi Karakter: Harus huruf A-Z, a-z, atau spasi
        if (!CryptoHelper.isValidText(plaintext)) {
            showToast("Gunakan huruf A-Z, a-z, atau spasi saja!")
            return
        }

        // Ambil rumus aktif saat pesan ini dikirim untuk dikunci di ChatMessage
        val formula = FormulaManager.getActiveFormula(this)

        // Proses Enkripsi Dinamis
        val ciphertext = CryptoHelper.encrypt(plaintext, formula.a, formula.b, formula.c, formula.d)
        val message = ChatMessage(
            senderId = currentUserId,
            senderName = currentUserName,
            receiverId = receiverUser.id,
            receiverName = receiverUser.name,
            ciphertext = ciphertext,
            plaintext = plaintext,
            coeffA = formula.a,
            coeffB = formula.b,
            coeffC = formula.c,
            coeffD = formula.d,
            dateTime = getReadableDateTime(Date()),
            timestamp = System.currentTimeMillis()
        )

        // Simpan Lokal & Kirim ke Firebase Realtime Database
        preferenceManager.saveChatMessage(message)
        FirebaseHelper.saveChatMessage(message)

        binding.inputMessage.text.clear()
    }

    private fun listenForMessages() {
        binding.progressBar.visibility = View.VISIBLE
        chatsListener = FirebaseHelper.listenForConversation(
            this,
            currentUserId,
            receiverUser.id,
            preferenceManager
        ) { messages ->
            binding.progressBar.visibility = View.GONE
            val adapter = ChatAdapter(
                messages,
                receiverProfileBitmap,
                currentUserId
            ) { clickedMessage ->
                showCalculationStepsDialog(clickedMessage)
            }

            binding.chatRecyclerView.adapter = adapter
            binding.chatRecyclerView.visibility = View.VISIBLE

            if (messages.isNotEmpty()) {
                binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
            }
        }
    }

    private fun showCalculationStepsDialog(message: ChatMessage) {
        val stepsText = CryptoHelper.getCalculationSteps(
            message.plaintext,
            message.ciphertext,
            message.coeffA,
            message.coeffB,
            message.coeffC,
            message.coeffD
        )
        DialogHelper.showInfoDialog(this, "Bedah Perhitungan Enkripsi & Deskripsi Pesan", stepsText)
    }

    private fun getReadableDateTime(date: Date): String {
        return SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
