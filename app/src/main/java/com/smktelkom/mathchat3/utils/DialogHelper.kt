package com.smktelkom.mathchat3.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.databinding.DialogAddContactBinding
import com.smktelkom.mathchat3.databinding.DialogCustomConfirmBinding
import com.smktelkom.mathchat3.databinding.DialogCustomFormulaBinding
import com.smktelkom.mathchat3.databinding.DialogCustomInfoBinding
import com.smktelkom.mathchat3.databinding.DialogCustomLanguageBinding
import com.smktelkom.mathchat3.databinding.DialogGoogleSigninBinding
import com.smktelkom.mathchat3.databinding.PopupCustomMenuBinding
import com.smktelkom.mathchat3.models.SavedContact
import com.smktelkom.mathchat3.models.User
import java.util.UUID

object DialogHelper {

    /**
     * Menampilkan Custom White Dialog untuk informasi (misal: Info Rumus / Rincian Kriptografi)
     */
    fun showInfoDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String = "Tutup"
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCustomInfoBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.88).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.textDialogTitle.text = title
        binding.textDialogMessage.text = message
        binding.buttonDialogClose.text = buttonText

        binding.buttonDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Dialog untuk Simpan Kontak Teman Baru (Save Nomor HP / Email)
     */
    fun showAddContactDialog(
        context: Context,
        preferenceManager: PreferenceManager,
        onContactSaved: (SavedContact) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogAddContactBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.buttonContactCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.buttonContactSave.setOnClickListener {
            val query = binding.inputContactQuery.text.toString().trim()
            val customAlias = binding.inputContactAlias.text.toString().trim()

            if (query.isEmpty()) {
                Toast.makeText(context, "Masukkan Nomor HP atau Email Teman!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cari di database users apakah ada akun dengan phoneNumber atau email tersebut
            val friendUser = preferenceManager.findUserByEmailOrPhone(query)
            if (friendUser == null) {
                Toast.makeText(
                    context,
                    "Nomor HP atau Email tersebut belum terdaftar di ChatMath!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val currentUserId = preferenceManager.getString(PreferenceManager.KEY_USER_ID) ?: ""
            if (friendUser.id == currentUserId) {
                Toast.makeText(context, "Anda tidak dapat menyimpan kontak akun sendiri!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val aliasName = if (customAlias.isNotEmpty()) customAlias else friendUser.name
            val contact = SavedContact(
                ownerUserId = currentUserId,
                contactUserId = friendUser.id,
                customAliasName = aliasName,
                phoneNumber = friendUser.phoneNumber,
                email = friendUser.email,
                imageBase64 = friendUser.imageBase64,
                isFavorite = false
            )

            preferenceManager.saveContact(currentUserId, contact)
            FirebaseHelper.saveContact(currentUserId, contact)

            Toast.makeText(context, "Kontak $aliasName berhasil disimpan!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onContactSaved(contact)
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Dialog untuk Pengaturan Rumus Linear Mode Hybrid (f(x) & g(x))
     */
    fun showCustomFormulaDialog(
        context: Context,
        onFormulaUpdated: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCustomFormulaBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val active = FormulaManager.getActiveFormula(context)
        binding.inputCoeffA.setText(active.a.toString())
        binding.inputConstB.setText(active.b.toString())
        binding.inputCoeffC.setText(active.c.toString())
        binding.inputConstD.setText(active.d.toString())

        fun updatePreview() {
            val a = binding.inputCoeffA.text.toString().toIntOrNull() ?: 0
            val b = binding.inputConstB.text.toString().toIntOrNull() ?: 0
            val c = binding.inputCoeffC.text.toString().toIntOrNull() ?: 0
            val d = binding.inputConstD.text.toString().toIntOrNull() ?: 0
            val m = a * c
            val k = (a * d) + b
            binding.textPreviewComposition.text =
                "Hasil Komposisi: (f ∘ g)(x) = ${FormulaManager.formatLinear(m, k)}\nInvers: (f ∘ g)⁻¹(c) = (c - ($k)) / $m"
        }

        updatePreview()

        binding.buttonSaveManual.setOnClickListener {
            val a = binding.inputCoeffA.text.toString().toIntOrNull()
            val b = binding.inputConstB.text.toString().toIntOrNull() ?: 0
            val c = binding.inputCoeffC.text.toString().toIntOrNull()
            val d = binding.inputConstD.text.toString().toIntOrNull() ?: 0

            if (a == null || a == 0 || c == null || c == 0) {
                Toast.makeText(
                    context,
                    "Koefisien pengali x (a dan c) tidak boleh 0 agar fungsi memiliki invers!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            FormulaManager.saveFormula(context, a, b, c, d)
            Toast.makeText(context, "Rumus kustom berhasil disimpan!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onFormulaUpdated()
        }

        binding.buttonRandomize.setOnClickListener {
            val newF = FormulaManager.randomizeFormula(context)
            binding.inputCoeffA.setText(newF.a.toString())
            binding.inputConstB.setText(newF.b.toString())
            binding.inputCoeffC.setText(newF.c.toString())
            binding.inputConstD.setText(newF.d.toString())
            updatePreview()
            Toast.makeText(context, "Rumus berhasil diacak!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onFormulaUpdated()
        }

        binding.buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Dialog untuk Masuk Cepat dengan Akun Google / No HP
     */
    fun showGoogleSignInDialog(
        context: Context,
        preferenceManager: PreferenceManager,
        onUserSelected: (User) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogGoogleSigninBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val existingUsers = preferenceManager.getAllUsers()
        for ((index, user) in existingUsers.withIndex()) {
            val rb = RadioButton(context).apply {
                id = View.generateViewId()
                text = "${user.name}\n(${if (user.email.isNotEmpty()) user.email else user.phoneNumber})"
                textSize = 13f
                setTextColor(Color.parseColor("#1F2937"))
                setPadding(20, 10, 20, 10)
                tag = user
            }
            binding.radioGroupGoogleAccounts.addView(rb)
            if (index == 0) rb.isChecked = true
        }

        val rbNew = RadioButton(context).apply {
            id = View.generateViewId()
            text = "+ Gunakan Akun Google / No HP Baru"
            textSize = 13f
            setTextColor(Color.parseColor("#881337"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(20, 10, 20, 10)
            tag = "NEW"
        }
        binding.radioGroupGoogleAccounts.addView(rbNew)

        binding.radioGroupGoogleAccounts.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val selectedRb = group.findViewById<RadioButton>(checkedId)
            if (selectedRb?.tag == "NEW") {
                binding.layoutNewAccountInputs.visibility = View.VISIBLE
            } else {
                binding.layoutNewAccountInputs.visibility = View.GONE
            }
        }

        binding.buttonGoogleCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.buttonGoogleProceed.setOnClickListener {
            val checkedId = binding.radioGroupGoogleAccounts.checkedRadioButtonId
            val selectedRb = binding.radioGroupGoogleAccounts.findViewById<RadioButton>(checkedId)
            if (selectedRb == null) {
                Toast.makeText(context, "Pilih akun terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedRb.tag == "NEW") {
                val name = binding.inputNewName.text.toString().trim()
                val email = binding.inputNewEmail.text.toString().trim()
                val phone = binding.inputNewPhone.text.toString().trim()

                if (name.isEmpty() || (email.isEmpty() && phone.isEmpty())) {
                    Toast.makeText(context, "Lengkapi Nama dan Email/No HP!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newUser = User(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phoneNumber = phone,
                    email = if (email.isNotEmpty()) email else "$phone@google.com",
                    password = "123456",
                    imageBase64 = AvatarGenerator.generateAvatarBase64(name)
                )

                preferenceManager.saveUser(newUser)
                FirebaseHelper.saveUser(newUser)
                dialog.dismiss()
                onUserSelected(newUser)
            } else {
                val user = selectedRb.tag as? User
                if (user != null) {
                    dialog.dismiss()
                    onUserSelected(user)
                }
            }
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Dialog untuk konfirmasi (misal: Confirmation Sign Out)
     */
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = context.getString(R.string.yes),
        negativeText: String = context.getString(R.string.cancel),
        onConfirm: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCustomConfirmBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.88).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        binding.textConfirmTitle.text = title
        binding.textConfirmMessage.text = message
        binding.buttonConfirmOk.text = positiveText
        binding.buttonConfirmCancel.text = negativeText

        binding.buttonConfirmCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.buttonConfirmOk.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Dialog untuk Pemilihan Bahasa (Bahasa Indonesia & English)
     */
    fun showLanguageDialog(
        context: Context,
        currentLang: String,
        onLanguageSelected: (String) -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCustomLanguageBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        dialog.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.88).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        if (currentLang == LocaleHelper.LANGUAGE_ENGLISH) {
            binding.radioEnglish.isChecked = true
        } else {
            binding.radioIndonesian.isChecked = true
        }

        binding.buttonLangCancel.setOnClickListener {
            dialog.dismiss()
        }

        binding.buttonLangApply.setOnClickListener {
            val selectedLang = if (binding.radioEnglish.isChecked) {
                LocaleHelper.LANGUAGE_ENGLISH
            } else {
                LocaleHelper.LANGUAGE_INDONESIAN
            }
            dialog.dismiss()
            onLanguageSelected(selectedLang)
        }

        dialog.show()
    }

    /**
     * Menampilkan Custom White Pop-Up Menu untuk tombol titik tiga di header
     */
    fun showCustomPopupMenu(
        context: Context,
        anchorView: View,
        onProfileClicked: () -> Unit,
        onLanguageClicked: () -> Unit
    ) {
        val binding = PopupCustomMenuBinding.inflate(LayoutInflater.from(context))
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.elevation = 16f
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.layoutMenuProfile.setOnClickListener {
            popupWindow.dismiss()
            onProfileClicked()
        }

        binding.layoutMenuLanguage.setOnClickListener {
            popupWindow.dismiss()
            onLanguageClicked()
        }

        // Tampilkan melayang di bawah tombol titik tiga
        popupWindow.showAsDropDown(anchorView, -40, 10)
    }
}
