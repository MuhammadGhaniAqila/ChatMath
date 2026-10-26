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
import android.widget.Toast
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.databinding.DialogCustomConfirmBinding
import com.smktelkom.mathchat3.databinding.DialogCustomInfoBinding
import com.smktelkom.mathchat3.databinding.DialogCustomLanguageBinding
import com.smktelkom.mathchat3.databinding.DialogFormulaSettingsBinding
import com.smktelkom.mathchat3.databinding.PopupCustomMenuBinding

object DialogHelper {

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

    fun showCustomFormulaDialog(
        context: Context,
        onFormulaUpdated: () -> Unit
    ) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogFormulaSettingsBinding.inflate(LayoutInflater.from(context))
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

        popupWindow.showAsDropDown(anchorView, -40, 10)
    }
}
