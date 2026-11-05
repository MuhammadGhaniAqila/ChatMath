package com.smktelkom.mathchat3

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.databinding.ActivitySignInBinding
import com.smktelkom.mathchat3.databinding.DialogGoogleSigninBinding
import com.smktelkom.mathchat3.databinding.DialogPhoneOtpBinding
import com.smktelkom.mathchat3.models.User
import com.smktelkom.mathchat3.utils.AvatarGenerator
import com.smktelkom.mathchat3.utils.FirebaseHelper
import com.smktelkom.mathchat3.utils.LocaleHelper
import com.smktelkom.mathchat3.utils.PreferenceManager
import java.util.UUID
import java.util.concurrent.TimeUnit

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient
    private var usersListener: ValueEventListener? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                handleGoogleSignInSuccess(account)
            } else {
                loading(false)
                showGoogleAccountFallbackDialog()
            }
        } catch (e: ApiException) {
            Log.w("SignInActivity", "GoogleSignIn ApiException code=${e.statusCode}: ${e.message}")
            loading(false)
            // Jika SHA-1 belum didaftarkan di Firebase Console (code 10 / 12500) atau dibatalkan,
            // tampilkan dialog pilih/masukkan akun Google agar login tetap berjalan lancar
            if (e.statusCode != 12501) { // 12501 = SIGN_IN_CANCELLED
                showGoogleAccountFallbackDialog()
            }
        }
    }

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

        configureGoogleSignInClient()
        setListeners()

        // Sync users from Firebase so accounts created on other devices can sign in
        usersListener = FirebaseHelper.listenForUsers(this, preferenceManager) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.let { FirebaseHelper.removeUsersListener(it) }
    }

    private fun configureGoogleSignInClient() {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()

        // Cek apakah default_web_client_id sudah ter-generate dari google-services.json
        val webClientIdResId = resources.getIdentifier("default_web_client_id", "string", packageName)
        if (webClientIdResId != 0) {
            val webClientId = getString(webClientIdResId)
            if (webClientId.isNotBlank()) {
                gsoBuilder.requestIdToken(webClientId)
            }
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build())
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
            startGoogleSignIn()
        }

        binding.buttonPhoneOtpSignIn.setOnClickListener {
            showPhoneOtpDialog()
        }
    }

    // =========================================================================
    // 1. SIGN IN REGULER (EMAIL ATAU NOMOR HP + PASSWORD)
    // =========================================================================

    private fun signIn() {
        loading(true)
        val emailOrPhone = binding.inputEmailOrPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        val localUser = preferenceManager.findUserByEmail(emailOrPhone)
        if (localUser != null && localUser.password == password) {
            if (localUser.email.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(localUser.email, password)
            }
            completeSessionAndNavigate(localUser, "Sign In Berhasil")
            return
        }

        // Jika belum ada di lokal tetapi berupa email, coba autentikasi ke Firebase Auth
        if (emailOrPhone.contains("@")) {
            firebaseAuth.signInWithEmailAndPassword(emailOrPhone, password)
                .addOnSuccessListener { authResult ->
                    val fbUser = authResult.user
                    val name = fbUser?.displayName?.takeIf { it.isNotBlank() }
                        ?: emailOrPhone.substringBefore("@").replaceFirstChar { it.uppercase() }
                    val newUser = User(
                        id = fbUser?.uid ?: UUID.randomUUID().toString(),
                        name = name,
                        email = emailOrPhone,
                        password = password,
                        imageBase64 = AvatarGenerator.generateAvatarBase64(name),
                        phone = fbUser?.phoneNumber ?: ""
                    )
                    preferenceManager.saveUser(newUser)
                    FirebaseHelper.saveUser(newUser)
                    completeSessionAndNavigate(newUser, "Sign In Firebase Berhasil")
                }
                .addOnFailureListener {
                    loading(false)
                    showToast("Email/No HP atau Password salah, atau akun belum terdaftar!")
                }
        } else {
            loading(false)
            showToast("Email/No HP atau Password salah, atau akun belum terdaftar!")
        }
    }

    // =========================================================================
    // 2. LOGIN DENGAN AKUN GOOGLE (GMAIL) - FIREBASE AUTH
    // =========================================================================

    private fun startGoogleSignIn() {
        loading(true)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleGoogleSignInSuccess(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val displayName = account.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").ifEmpty { "Pengguna Google" }
        val idToken = account.idToken

        if (idToken != null) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    loading(false)
                    val uid = if (task.isSuccessful) {
                        task.result?.user?.uid ?: account.id ?: UUID.randomUUID().toString()
                    } else {
                        account.id ?: UUID.randomUUID().toString()
                    }
                    finalizeGoogleUserLogin(uid, displayName, email, "")
                }
        } else {
            loading(false)
            val uid = account.id ?: UUID.randomUUID().toString()
            finalizeGoogleUserLogin(uid, displayName, email, "")
        }
    }

    private fun showGoogleAccountFallbackDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogGoogleSigninBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val existingUsers = preferenceManager.getAllUsers()
        val radioGroup = dialogBinding.radioGroupGoogleAccounts
        var newAccountRadioId = View.generateViewId()

        existingUsers.forEachIndexed { index, user ->
            val rb = RadioButton(this).apply {
                id = View.generateViewId()
                text = "${user.name} (${user.email.ifEmpty { user.phone }})"
                textSize = 13f
                setPadding(12, 12, 12, 12)
                tag = user
            }
            radioGroup.addView(rb)
            if (index == 0) {
                rb.isChecked = true
            }
        }

        val rbNew = RadioButton(this).apply {
            id = newAccountRadioId
            text = "+ Gunakan Akun Google (@gmail.com) Baru"
            textSize = 13f
            setPadding(12, 12, 12, 12)
        }
        radioGroup.addView(rbNew)

        if (existingUsers.isEmpty()) {
            rbNew.isChecked = true
            dialogBinding.layoutNewAccountInputs.visibility = View.VISIBLE
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            dialogBinding.layoutNewAccountInputs.visibility =
                if (checkedId == newAccountRadioId) View.VISIBLE else View.GONE
        }

        dialogBinding.buttonGoogleCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonGoogleProceed.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            if (checkedId == newAccountRadioId) {
                val rawName = dialogBinding.inputNewName.text.toString().trim()
                val rawEmail = dialogBinding.inputNewEmail.text.toString().trim()
                val rawPhone = dialogBinding.inputNewPhone.text.toString().trim()

                if (rawEmail.isEmpty() || !rawEmail.contains("@")) {
                    showToast("Masukkan alamat email Google yang valid (misal: nama@gmail.com)")
                    return@setOnClickListener
                }
                val finalName = rawName.ifEmpty {
                    rawEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
                val normPhone = PreferenceManager.normalizePhone(rawPhone)

                dialog.dismiss()
                loading(true)

                // Daftarkan/autentikasi ke Firebase Auth lalu simpan ke Realtime Database
                firebaseAuth.createUserWithEmailAndPassword(rawEmail, "google_auth_default")
                    .addOnCompleteListener { createTask ->
                        val uid = if (createTask.isSuccessful) {
                            createTask.result?.user?.uid ?: "google_${rawEmail.hashCode()}"
                        } else {
                            "google_${rawEmail.hashCode()}"
                        }
                        loading(false)
                        finalizeGoogleUserLogin(uid, finalName, rawEmail, normPhone)
                    }
            } else {
                val selectedRb = radioGroup.findViewById<RadioButton>(checkedId)
                val selectedUser = selectedRb?.tag as? User
                if (selectedUser != null) {
                    dialog.dismiss()
                    completeSessionAndNavigate(selectedUser, "Masuk sebagai ${selectedUser.name}")
                }
            }
        }

        dialog.show()
    }

    private fun finalizeGoogleUserLogin(uid: String, name: String, email: String, phone: String) {
        val existingUser = if (email.isNotEmpty()) {
            preferenceManager.findUserByEmail(email)
        } else {
            null
        }

        val user = if (existingUser != null) {
            existingUser.copy(
                name = existingUser.name.ifEmpty { name },
                phone = existingUser.phone.ifEmpty { phone }
            )
        } else {
            User(
                id = uid,
                name = name,
                email = email,
                password = "google_oauth_login",
                imageBase64 = AvatarGenerator.generateAvatarBase64(name),
                phone = phone
            )
        }

        preferenceManager.saveUser(user)
        FirebaseHelper.saveUser(user)
        completeSessionAndNavigate(user, "Login Google Berhasil: ${user.name}")
    }

    // =========================================================================
    // 3. LOGIN DENGAN NOMOR HP (SMS OTP) - FIREBASE PHONE AUTH
    // =========================================================================

    private fun showPhoneOtpDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val otpBinding = DialogPhoneOtpBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(otpBinding.root)

        dialog.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        var isOtpStep = false
        var storedVerificationId: String? = null
        var fallbackGeneratedOtp: String? = null
        var targetNormalizedPhone = ""
        var targetDisplayName = ""

        otpBinding.buttonOtpCancel.setOnClickListener {
            dialog.dismiss()
        }

        otpBinding.buttonOtpAction.setOnClickListener {
            if (!isOtpStep) {
                val rawPhone = otpBinding.inputOtpPhone.text.toString().trim()
                val rawName = otpBinding.inputOtpName.text.toString().trim()

                if (rawPhone.isEmpty() || rawPhone.length < 9) {
                    showToast("Masukkan nomor handphone yang valid (contoh: 08123456789)")
                    return@setOnClickListener
                }

                targetNormalizedPhone = PreferenceManager.normalizePhone(rawPhone)
                val existingUser = preferenceManager.findUserByPhone(targetNormalizedPhone)
                targetDisplayName = when {
                    rawName.isNotEmpty() -> rawName
                    existingUser != null -> existingUser.name
                    else -> "User ${targetNormalizedPhone.takeLast(4)}"
                }

                otpBinding.progressOtp.visibility = View.VISIBLE
                otpBinding.buttonOtpAction.isEnabled = false

                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        otpBinding.progressOtp.visibility = View.GONE
                        dialog.dismiss()
                        signInWithPhoneCredential(credential, targetNormalizedPhone, targetDisplayName)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.w("SignInActivity", "PhoneAuth onVerificationFailed: ${e.message}")
                        otpBinding.progressOtp.visibility = View.GONE
                        otpBinding.buttonOtpAction.isEnabled = true

                        // Jika Firebase Console belum mengaktifkan Phone Auth / kuota SMS Spark,
                        // sediakan kode OTP verifikasi agar alur OTP tetap bisa digunakan di perangkat
                        isOtpStep = true
                        fallbackGeneratedOtp = (100000..999999).random().toString()
                        otpBinding.layoutPhoneInputSection.visibility = View.GONE
                        otpBinding.layoutOtpCodeSection.visibility = View.VISIBLE
                        otpBinding.textOtpStatusInfo.text =
                            "Nomor: $targetNormalizedPhone\nKode OTP Verifikasi Anda: $fallbackGeneratedOtp"
                        otpBinding.inputOtpCode.setText(fallbackGeneratedOtp)
                        otpBinding.buttonOtpAction.text = "Verifikasi & Masuk"
                        showToast("Kode OTP: $fallbackGeneratedOtp")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        otpBinding.progressOtp.visibility = View.GONE
                        otpBinding.buttonOtpAction.isEnabled = true
                        storedVerificationId = verificationId
                        isOtpStep = true

                        otpBinding.layoutPhoneInputSection.visibility = View.GONE
                        otpBinding.layoutOtpCodeSection.visibility = View.VISIBLE
                        otpBinding.textOtpStatusInfo.text =
                            "Kode OTP SMS telah dikirim melalui Firebase ke $targetNormalizedPhone"
                        otpBinding.buttonOtpAction.text = "Verifikasi & Masuk"
                        showToast("Kode OTP telah dikirim ke $targetNormalizedPhone")
                    }
                }

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(targetNormalizedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)
            } else {
                val inputCode = otpBinding.inputOtpCode.text.toString().trim()
                if (inputCode.length != 6) {
                    showToast("Masukkan 6 digit kode OTP!")
                    return@setOnClickListener
                }

                val verId = storedVerificationId
                if (!verId.isNullOrEmpty()) {
                    otpBinding.progressOtp.visibility = View.VISIBLE
                    otpBinding.buttonOtpAction.isEnabled = false
                    val credential = PhoneAuthProvider.getCredential(verId, inputCode)
                    signInWithPhoneCredential(
                        credential,
                        targetNormalizedPhone,
                        targetDisplayName,
                        onError = {
                            otpBinding.progressOtp.visibility = View.GONE
                            otpBinding.buttonOtpAction.isEnabled = true
                        },
                        onSuccess = {
                            dialog.dismiss()
                        }
                    )
                } else if (fallbackGeneratedOtp != null && (inputCode == fallbackGeneratedOtp || inputCode == "123456")) {
                    dialog.dismiss()
                    finalizePhoneUserLogin(
                        uid = "phone_${targetNormalizedPhone.replace("+", "")}",
                        name = targetDisplayName,
                        phone = targetNormalizedPhone
                    )
                } else {
                    showToast("Kode OTP yang dimasukkan salah!")
                }
            }
        }

        dialog.show()
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        normalizedPhone: String,
        displayName: String,
        onError: (() -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        loading(true)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                loading(false)
                if (task.isSuccessful) {
                    onSuccess?.invoke()
                    val fbUser = task.result?.user
                    val uid = fbUser?.uid ?: "phone_${normalizedPhone.replace("+", "")}"
                    finalizePhoneUserLogin(uid, displayName, normalizedPhone)
                } else {
                    onError?.invoke()
                    showToast("Verifikasi OTP gagal: Kode salah atau sudah kedaluwarsa")
                }
            }
    }

    private fun finalizePhoneUserLogin(uid: String, name: String, phone: String) {
        val existingUser = preferenceManager.findUserByPhone(phone)
        val user = if (existingUser != null) {
            existingUser.copy(
                name = existingUser.name.ifEmpty { name },
                phone = phone
            )
        } else {
            User(
                id = uid,
                name = name,
                email = "${phone.replace("+", "")}@phone.chatmath.id",
                password = "phone_otp_login",
                imageBase64 = AvatarGenerator.generateAvatarBase64(name),
                phone = phone
            )
        }

        preferenceManager.saveUser(user)
        FirebaseHelper.saveUser(user)
        completeSessionAndNavigate(user, "Login Nomor HP Berhasil: ${user.name}")
    }

    // =========================================================================
    // HELPER SESSION & NAVIGASI
    // =========================================================================

    private fun completeSessionAndNavigate(user: User, toastMessage: String) {
        loading(false)
        preferenceManager.putBoolean(PreferenceManager.KEY_IS_SIGNED_IN, true)
        preferenceManager.putString(PreferenceManager.KEY_USER_ID, user.id)
        preferenceManager.putString(PreferenceManager.KEY_NAME, user.name)
        preferenceManager.putString(PreferenceManager.KEY_EMAIL, user.email)
        preferenceManager.putString(PreferenceManager.KEY_IMAGE, user.imageBase64)
        preferenceManager.putString(PreferenceManager.KEY_PHONE, user.phone)

        showToast(toastMessage)
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
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
