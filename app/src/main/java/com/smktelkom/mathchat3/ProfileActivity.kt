package com.smktelkom.mathchat3

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.smktelkom.mathchat3.databinding.ActivityProfileBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.DialogHelper
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager
import java.io.InputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private var encodedImage: String? = null
    private var currentUserId: String = ""
    private var currentUserEmail: String = ""

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    encodedImage = AvatarGenerator.bitmapToBase64(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)

        // Penanganan System Window Insets agar Header tidak tertutup Status Bar / Kamera
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        loadProfileDetails()
        setListeners()
    }

    private fun loadProfileDetails() {
        currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""
        currentUserEmail = preferenceManager.getString(PreferenceManager.KEY_EMAIL) ?: ""
        val name = preferenceManager.getString(PreferenceManager.KEY_NAME) ?: ""
        val imageBase64 = preferenceManager.getString(PreferenceManager.KEY_IMAGE) ?: ""

        binding.inputName.setText(name)
        binding.inputEmail.setText(currentUserEmail) // Strictly Disabled / Non-Editable in XML!

        encodedImage = imageBase64
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
        binding.imageBack.setOnClickListener {
            finish()
        }

        binding.layoutImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }

        binding.buttonSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        // Tombol Log Out di bagian paling bawah
        binding.buttonSignOut.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    private fun saveProfileChanges() {
        val newName = binding.inputName.text.toString().trim()
        if (newName.isEmpty()) {
            showToast("Nama tidak boleh kosong!")
            return
        }

        val finalImageBase64 = encodedImage ?: AvatarGenerator.generateAvatarBase64(newName)

        // Email tetap menggunakan currentUserEmail yang paten
        val updatedUser = User(
            id = currentUserId,
            name = newName,
            email = currentUserEmail,
            password = preferenceManager.findUserByEmail(currentUserEmail)?.password ?: "",
            imageBase64 = finalImageBase64
        )

        // Simpan ke PreferenceManager lokal dan Firebase
        preferenceManager.putString(PreferenceManager.KEY_NAME, newName)
        preferenceManager.putString(PreferenceManager.KEY_IMAGE, finalImageBase64)
        preferenceManager.saveUser(updatedUser)
        FirebaseHelper.saveUser(updatedUser)

        showToast(getString(R.string.profile_updated_success))
        finish()
    }

    private fun showSignOutConfirmation() {
        DialogHelper.showConfirmDialog(
            this,
            title = getString(R.string.menu_sign_out),
            message = getString(R.string.sign_out_confirmation),
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.cancel)
        ) {
            signOut()
        }
    }

    private fun signOut() {
        Toast.makeText(applicationContext, "Signing out...", Toast.LENGTH_SHORT).show()
        // HANYA bersihkan file Sesi "ChatMathSession", JANGAN hapus "ChatMathDatabase" atau Firebase
        preferenceManager.clearSession()
        val intent = Intent(applicationContext, SignInActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
