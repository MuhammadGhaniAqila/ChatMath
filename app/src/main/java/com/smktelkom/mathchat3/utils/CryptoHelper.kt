package com.smktelkom.mathchat3.utils

/**
 * Logika Kriptografi Mode Hybrid Dinamis
 * Komposisi 2 Fungsi Linear f(x) = a*x + b dan g(x) = c*x + d
 * Sesuai Handout Proyek Enkripsi & Panduan Mode Hybrid SMK Telkom Purwokerto
 */
object CryptoHelper {

    fun isValidText(text: String): Boolean {
        for (ch in text) {
            if (!ch.isLetter() && ch != ' ') {
                return false
            }
        }
        return true
    }

    fun charToX(c: Char): Int {
        return when {
            c in 'A'..'Z' -> c - 'A' + 1
            c in 'a'..'z' -> -(c - 'a' + 1)
            c == ' ' -> 0
            else -> 0
        }
    }

    fun xToChar(x: Int): Char {
        return when {
            x in 1..26 -> ('A' + x - 1)
            x in -26..-1 -> ('a' + (-x) - 1)
            x == 0 -> ' '
            else -> '?'
        }
    }

    /**
     * Enkripsi Komposisi 2 Fungsi Dinamis: c_sandi = (f o g)(x) = M*x + K
     * dengan M = a * c dan K = (a * d) + b
     */
    fun encrypt(plaintext: String, a: Int, b: Int, c: Int, d: Int): String {
        if (plaintext.isEmpty()) return ""
        val cipherList = mutableListOf<Int>()
        for (ch in plaintext) {
            val x = charToX(ch)
            val gx = c * x + d
            val cipher = a * gx + b // M*x + K
            cipherList.add(cipher)
        }
        return cipherList.joinToString(" ")
    }

    /**
     * Dekripsi Invers Komposisi Dinamis: p = (c_sandi - K) / M
     * Menggunakan koefisien (a, b, c, d) khusus yang dikunci pada pesan tersebut.
     */
    fun decrypt(ciphertext: String, a: Int, b: Int, c: Int, d: Int): String {
        if (ciphertext.trim().isEmpty()) return ""
        val m = a * c
        val k = (a * d) + b
        if (m == 0) return "" // Keamanan pembagian dengan nol

        val sb = StringBuilder()
        val tokens = ciphertext.trim().split("\\s+".toRegex())
        for (token in tokens) {
            val cipher = token.toIntOrNull() ?: continue
            val p = (cipher - k) / m
            val ch = xToChar(p)
            sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * Menghasilkan penjabaran aljabar dan rincian langkah perhitungan matematika untuk Penilaian Guru
     */
    fun getCalculationSteps(plaintext: String, ciphertext: String, a: Int, b: Int, c: Int, d: Int): String {
        val m = a * c
        val k = (a * d) + b
        val fStr = FormulaManager.formatLinear(a, b)
        val gStr = FormulaManager.formatLinear(c, d)
        val fogStr = FormulaManager.formatLinear(m, k)

        val sb = StringBuilder()
        sb.append("DOKUMEN BEDAH KRIPTOGRAFI HYBRID DINAMIS\n")
        sb.append("Aplikasi ChatMath — SMK Telkom Purwokerto\n")
        sb.append("=========================================\n\n")
        sb.append("RUMUS DILOCK PADA PESAN INI:\n")
        sb.append("1. Fungsi f(x) = $fStr\n")
        sb.append("2. Fungsi g(x) = $gStr\n")
        sb.append("3. Komposisi Enkripsi (f ∘ g)(x) = $fogStr\n")
        sb.append("4. Dekripsi Invers (f ∘ g)⁻¹(c) = (c - ($k)) / $m\n\n")
        sb.append("RINCIAN PER KARAKTER:\n")
        sb.append("-----------------------------------------\n")

        val cipherTokens = ciphertext.trim().split("\\s+".toRegex())
        for (i in plaintext.indices) {
            val ch = plaintext[i]
            val x = charToX(ch)
            val gx = c * x + d
            val cCalc = a * gx + b
            val cGiven = if (i < cipherTokens.size) cipherTokens[i] else cCalc.toString()
            val cInt = cGiven.toIntOrNull() ?: cCalc

            // Tahap 1 dekripsi: y1 = (c - b)/a
            val y1 = if (a != 0) (cInt - b) / a else 0
            // Tahap 2 dekripsi: p = (y1 - d)/c
            val pDecrypted = if (c != 0) (y1 - d) / c else 0
            val chDecrypted = xToChar(pDecrypted)

            val displayChar = if (ch == ' ') "[Spasi]" else "'$ch'"
            sb.append("${i + 1}. Karakter: $displayChar\n")
            sb.append("   a) Kuantifikasi x = $x\n")
            sb.append("   b) Tahap 1 g($x): $c($x) + $d = $gx\n")
            sb.append("   c) Enkripsi f($gx): $a($gx) + $b = $cCalc\n")
            sb.append("   d) Cipherteks = $cCalc\n")
            sb.append("   e) Dekripsi Invers Tahap 1 f⁻¹($cInt): ($cInt - ($b)) / $a = $y1\n")
            sb.append("   f) Dekripsi Invers Tahap 2 g⁻¹($y1): ($y1 - ($d)) / $c = $pDecrypted -> '$chDecrypted'\n\n")
        }

        sb.append("-----------------------------------------\n")
        sb.append("Hasil Plainteks  : \"$plaintext\"\n")
        sb.append("Hasil Cipherteks : \"$ciphertext\"\n")
        return sb.toString()
    }
}
