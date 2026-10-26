package com.smktelkom.mathchat3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.databinding.ActivitySignInBinding
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager
    private var usersListener: ValueEventListener? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(applicationContext)

        if (preferenceManager.getBoolean(PreferenceManager.KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        setListeners()

        // Sync users from Firebase so accounts created on other devices can sign in
        usersListener = FirebaseHelper.listenForUsers(this, preferenceManager) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let { FirebaseHelper.removeUsersListener(it) }
    }

    private fun setListeners() {
        binding.textCreateNewAccount.setOnClickListener {
            startActivity(Intent(applicationContext, SignUpActivity::class.java))
        }

        binding.buttonSignIn.setOnClickListener {
            if (isValidSignInDetails()) {
                signIn()
            }
        }

        binding.buttonGoogleSignIn.setOnClickListener {
            showToast("Fitur Login Google siap disambungkan ke Firebase Auth!")
        }

        binding.buttonPhoneOtpSignIn.setOnClickListener {
            showToast("Fitur Login Nomor HP (OTP) siap disambungkan ke Firebase Auth!")
        }
    }

    private fun signIn() {
        loading(true)
        val emailOrPhone = binding.inputEmailOrPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        val user = preferenceManager.findUserByEmail(emailOrPhone)
        if (user != null && user.password == password) {
            preferenceManager.putBoolean(PreferenceManager.KEY_IS_SIGNED_IN, true)
            preferenceManager.putString(PreferenceManager.KEY_USER_ID, user.id)
            preferenceManager.putString(PreferenceManager.KEY_NAME, user.name)
            preferenceManager.putString(PreferenceManager.KEY_EMAIL, user.email)
            preferenceManager.putString(PreferenceManager.KEY_IMAGE, user.imageBase64)

            showToast("Sign In Berhasil")
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            loading(false)
            showToast("Email/No HP atau Password salah, atau akun belum terdaftar!")
        }
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSignIn.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignIn.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidSignInDetails(): Boolean {
        return when {
            binding.inputEmailOrPhone.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Email atau Nomor Handphone")
                false
            }
            binding.inputPassword.text.toString().trim().isEmpty() -> {
                showToast("Masukkan Password")
                false
            }
            else -> true
        }
    }
}
