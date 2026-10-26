package com.smktelkom.mathchat3

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.smktelkom.mathchat3.databinding.ActivitySignUpBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager
import java.io.InputStream
import java.util.UUID

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var preferenceManager: PreferenceManager
    private var encodedImage: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    binding.textAddImage.visibility = View.GONE
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
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setListeners()
    }

    private fun setListeners() {
        binding.textSignIn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonSignUp.setOnClickListener {
            if (isValidSignUpDetails()) {
                signUp()
            }
        }

        binding.layoutImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun signUp() {
        loading(true)
        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (preferenceManager.findUserByEmail(email) != null) {
            loading(false)
            showToast("Akun sudah terdaftar, silakan langsung Sign In!")
            return
        }

        val finalImageBase64 = encodedImage ?: AvatarGenerator.generateAvatarBase64(name)

        val user = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            password = password,
            imageBase64 = finalImageBase64
        )

        preferenceManager.saveUser(user)
        FirebaseHelper.saveUser(user)

        preferenceManager.putBoolean(PreferenceManager.KEY_IS_SIGNED_IN, true)
        preferenceManager.putString(PreferenceManager.KEY_USER_ID, user.id)
        preferenceManager.putString(PreferenceManager.KEY_NAME, user.name)
        preferenceManager.putString(PreferenceManager.KEY_EMAIL, user.email)
        preferenceManager.putString(PreferenceManager.KEY_IMAGE, user.imageBase64)

        showToast("Pendaftaran Berhasil")
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignUp.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignUp.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignUpDetails(): Boolean {
        return when {
            binding.inputName.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Nama")
                false
            }
            binding.inputPhone.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Nomor Handphone")
                false
            }
            binding.inputEmail.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Email")
                false
            }
            binding.inputPassword.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Password")
                false
            }
            binding.inputConfirmPassword.text.toString().trim().isEmpty() -> {
                showToast("Konfirmasi Password Anda")
                false
            }
            binding.inputPassword.text.toString().trim() != binding.inputConfirmPassword.text.toString().trim() -> {
                showToast("Password dan Konfirmasi Password harus sama")
                false
            }
            preferenceManager.findUserByEmail(binding.inputEmail.text.toString().trim()) != null -> {
                showToast("Akun sudah terdaftar, silakan langsung Sign In!")
                false
            }
            else -> true
        }
    }
}
