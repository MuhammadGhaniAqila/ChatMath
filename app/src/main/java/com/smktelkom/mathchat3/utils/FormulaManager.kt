package com.smktelkom.mathchat3.utils

import android.content.Context
import android.widget.Toast

object FormulaManager {

    // Companion object flag untuk melacak apakah aplikasi baru saja dibuka dari keadaan tertutup
    var isSessionFormulaInitialized = false

    private const val PREF_FORMULA = "ChatMathFormula"
    private const val KEY_A = "coeff_a"
    private const val KEY_B = "coeff_b"
    private const val KEY_C = "coeff_c"
    private const val KEY_D = "coeff_d"

    // Daftar acak koefisien
    private val LIST_A_C = listOf(2, 3, 4, 5, -2, -3)
    private val LIST_B_D = listOf(-5, -4, -3, -2, -1, 1, 2, 3, 4, 5)

    data class Formula(val a: Int, val b: Int, val c: Int, val d: Int) {
        val m: Int get() = a * c
        val k: Int get() = (a * d) + b
    }

    /**
     * Memeriksa dan mengacak rumus otomatis setiap kali aplikasi dibuka dari keadaan tertutup
     */
    fun checkAndInitializeSessionFormula(context: Context): Formula {
        if (!isSessionFormulaInitialized) {
            isSessionFormulaInitialized = true
            val newFormula = randomizeFormula(context)
            val fStr = formatLinear(newFormula.a, newFormula.b)
            val gStr = formatLinear(newFormula.c, newFormula.d)
            Toast.makeText(
                context.applicationContext,
                "Sesi Baru: Rumus diacak menjadi f(x)=$fStr & g(x)=$gStr",
                Toast.LENGTH_LONG
            ).show()
            return newFormula
        }
        return getActiveFormula(context)
    }

    /**
     * Mengacak koefisien (a, b, c, d) secara otomatis
     */
    fun randomizeFormula(context: Context): Formula {
        val a = LIST_A_C.random()
        val b = LIST_B_D.random()
        val c = LIST_A_C.random()
        val d = LIST_B_D.random()

        return saveFormula(context, a, b, c, d)
    }

    /**
     * Menyimpan rumus kustom dari input manual pengguna / guru
     */
    fun saveFormula(context: Context, a: Int, b: Int, c: Int, d: Int): Formula {
        val prefs = context.getSharedPreferences(PREF_FORMULA, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_A, a)
            .putInt(KEY_B, b)
            .putInt(KEY_C, c)
            .putInt(KEY_D, d)
            .apply()
        return Formula(a, b, c, d)
    }

    /**
     * Mengambil koefisien rumus yang aktif saat ini
     */
    fun getActiveFormula(context: Context): Formula {
        val prefs = context.getSharedPreferences(PREF_FORMULA, Context.MODE_PRIVATE)
        val a = prefs.getInt(KEY_A, 3)
        val b = prefs.getInt(KEY_B, 2)
        val c = prefs.getInt(KEY_C, 2)
        val d = prefs.getInt(KEY_D, -1)
        return Formula(a, b, c, d)
    }

    /**
     * Format teks persamaan linear (a*x + b) menjadi string yang bersih
     */
    fun formatLinear(coeff: Int, constVal: Int): String {
        val coeffStr = when (coeff) {
            1 -> "x"
            -1 -> "-x"
            else -> "${coeff}x"
        }
        return when {
            constVal > 0 -> "$coeffStr + $constVal"
            constVal < 0 -> "$coeffStr - ${-constVal}"
            else -> coeffStr
        }
    }

    /**
     * Teks ringkas banner rumus aktif untuk ditampilkan di header
     */
    fun getFormulaBannerText(context: Context): String {
        val f = getActiveFormula(context)
        val fStr = formatLinear(f.a, f.b)
        val gStr = formatLinear(f.c, f.d)
        val fogStr = formatLinear(f.m, f.k)
        return "Rumus: f(x)=$fStr | g(x)=$gStr ➔ (f∘g)(x)=$fogStr"
    }
}
