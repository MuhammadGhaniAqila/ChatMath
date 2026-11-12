package com.smktelkom.mathchat3.utils

import android.content.Context
import android.content.SharedPreferences
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User
import org.json.JSONArray
import org.json.JSONObject

class PreferenceManager(context: Context) {

    // File 1: Khusus menyimpan SESI login yang sedang aktif
    private val sessionPrefs: SharedPreferences =
        context.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)

    // File 2: Khusus menyimpan DATABASE permanen seluruh akun terdaftar & pesan
    private val databasePrefs: SharedPreferences =
        context.getSharedPreferences(PREF_DATABASE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_SESSION_NAME = "ChatMathSession"
        private const val PREF_DATABASE_NAME = "ChatMathDatabase"

        // Session Keys
        const val KEY_IS_SIGNED_IN = "isSignedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_IMAGE = "image"
        const val KEY_PHONE = "phone"

        // Permanent Database Keys
        private const val KEY_USERS_LIST_JSON = "registered_users_json"
        private const val KEY_MESSAGES_LIST_JSON = "messages_list_json"
        private const val KEY_FAVORITES_JSON = "user_favorites_json"

        fun normalizePhone(rawPhone: String): String {
            val cleaned = rawPhone.replace("[^0-9+]".toRegex(), "").trim()
            return when {
                cleaned.isEmpty() -> ""
                cleaned.startsWith("+62") -> cleaned
                cleaned.startsWith("62") -> "+$cleaned"
                cleaned.startsWith("0") -> "+62${cleaned.substring(1)}"
                !cleaned.startsWith("+") -> "+62$cleaned"
                else -> cleaned
            }
        }
    }

    init {
        // Otomatis masukkan 2 Akun Demo Bawaan jika database users masih kosong saat pertama kali aplikasi dijalankan
        seedPreloadedDemoAccounts()
    }

    // =========================================================================
    // MANAGEMENT SESSION (ChatMathSession)
    // =========================================================================

    fun putBoolean(key: String, value: Boolean) {
        sessionPrefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String): Boolean {
        return sessionPrefs.getBoolean(key, false)
    }

    fun putString(key: String, value: String) {
        sessionPrefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String? {
        return sessionPrefs.getString(key, null)
    }

    /**
     * PENTING: Saat Logout, HANYA bersihkan file Sesi "ChatMathSession".
     * JANGAN PERNAH menghapus "ChatMathDatabase" agar data akun dan riwayat chat tetap tersimpan permanen!
     */
    fun clearSession() {
        sessionPrefs.edit().clear().apply()
    }

    // =========================================================================
    // MANAGEMENT PERMANENT DATABASE (ChatMathDatabase)
    // =========================================================================

    private fun seedPreloadedDemoAccounts() {
        val existingUsers = getAllUsers()
        if (existingUsers.isEmpty()) {
            val demo1 = User(
                id = "demo_siswa1",
                name = "Siswa Telkom 1",
                email = "siswa1@telkom.sch.id",
                password = "123456",
                imageBase64 = AvatarGenerator.generateAvatarBase64("Siswa Telkom 1"),
                phone = "+628123456001"
            )
            val demo2 = User(
                id = "demo_siswa2",
                name = "Siswa Telkom 2",
                email = "siswa2@telkom.sch.id",
                password = "123456",
                imageBase64 = AvatarGenerator.generateAvatarBase64("Siswa Telkom 2"),
                phone = "+628123456002"
            )
            saveUser(demo1)
            saveUser(demo2)
            FirebaseHelper.saveUser(demo1)
            FirebaseHelper.saveUser(demo2)
        }
    }

    fun saveUser(user: User) {
        val users = getAllUsers().toMutableList()
        val normPhone = normalizePhone(user.phone)
        users.removeAll {
            it.id == user.id ||
                (user.email.isNotEmpty() && it.email.equals(user.email, ignoreCase = true)) ||
                (normPhone.isNotEmpty() && normalizePhone(it.phone) == normPhone)
        }
        users.add(user)

        val jsonArray = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("email", u.email)
                put("password", u.password)
                put("imageBase64", u.imageBase64)
                put("phone", u.phone)
            }
            jsonArray.put(obj)
        }
        databasePrefs.edit().putString(KEY_USERS_LIST_JSON, jsonArray.toString()).apply()
    }

    fun getAllUsers(): List<User> {
        val jsonStr = databasePrefs.getString(KEY_USERS_LIST_JSON, null) ?: return emptyList()
        val list = mutableListOf<User>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    User(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        email = obj.optString("email"),
                        password = obj.optString("password"),
                        imageBase64 = obj.optString("imageBase64"),
                        phone = obj.optString("phone", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun findUserByEmail(emailOrPhone: String): User? {
        val trimmed = emailOrPhone.trim()
        if (trimmed.isEmpty()) return null
        val normPhone = normalizePhone(trimmed)
        return getAllUsers().firstOrNull { u ->
            (u.email.isNotEmpty() && u.email.equals(trimmed, ignoreCase = true)) ||
                (u.phone.isNotEmpty() && (u.phone.equals(trimmed, ignoreCase = true) || normalizePhone(u.phone) == normPhone))
        }
    }

    fun findUserByPhone(phone: String): User? {
        val normPhone = normalizePhone(phone)
        if (normPhone.isEmpty()) return null
        return getAllUsers().firstOrNull { u ->
            u.phone.isNotEmpty() && normalizePhone(u.phone) == normPhone
        }
    }

    fun getCurrentUser(): User? {
        val id = getString(KEY_USER_ID) ?: return null
        val name = getString(KEY_NAME) ?: ""
        val email = getString(KEY_EMAIL) ?: ""
        val image = getString(KEY_IMAGE) ?: ""
        val phone = getString(KEY_PHONE) ?: ""
        return User(id = id, name = name, email = email, imageBase64 = image, phone = phone)
    }

    // =========================================================================
    // MANAGEMENT FAVORITE CONTACTS (ChatMathDatabase)
    // =========================================================================

    fun getFavoriteUserIds(): Set<String> {
        val currentUserId = getString(KEY_USER_ID) ?: return emptySet()
        val jsonStr = databasePrefs.getString("${KEY_FAVORITES_JSON}_$currentUserId", null) ?: return emptySet()
        val set = mutableSetOf<String>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                set.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return set
    }

    fun isUserFavorite(targetUserId: String): Boolean {
        return getFavoriteUserIds().contains(targetUserId)
    }

    fun toggleUserFavorite(targetUserId: String): Boolean {
        val currentUserId = getString(KEY_USER_ID) ?: return false
        val favorites = getFavoriteUserIds().toMutableSet()
        val isNowFav = if (favorites.contains(targetUserId)) {
            favorites.remove(targetUserId)
            false
        } else {
            favorites.add(targetUserId)
            true
        }
        val jsonArray = JSONArray()
        for (id in favorites) {
            jsonArray.put(id)
        }
        databasePrefs.edit().putString("${KEY_FAVORITES_JSON}_$currentUserId", jsonArray.toString()).apply()
        FirebaseHelper.saveFavoriteStatus(currentUserId, targetUserId, isNowFav)
        return isNowFav
    }

    fun syncFavorites(favSet: Set<String>) {
        val currentUserId = getString(KEY_USER_ID) ?: return
        val jsonArray = JSONArray()
        for (id in favSet) {
            jsonArray.put(id)
        }
        databasePrefs.edit().putString("${KEY_FAVORITES_JSON}_$currentUserId", jsonArray.toString()).apply()
    }

    // =========================================================================
    // MANAGEMENT CHAT MESSAGES (ChatMathDatabase)
    // =========================================================================

    fun saveChatMessage(message: ChatMessage) {
        val messages = getAllChatMessages().toMutableList()
        messages.removeAll { it.id == message.id }
        messages.add(message)

        val jsonArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("senderId", m.senderId)
                put("senderName", m.senderName)
                put("receiverId", m.receiverId)
                put("receiverName", m.receiverName)
                put("ciphertext", m.ciphertext)
                put("plaintext", m.plaintext)
                put("coeffA", m.coeffA)
                put("coeffB", m.coeffB)
                put("coeffC", m.coeffC)
                put("coeffD", m.coeffD)
                put("dateTime", m.dateTime)
                put("timestamp", m.timestamp)
            }
            jsonArray.put(obj)
        }
        databasePrefs.edit().putString(KEY_MESSAGES_LIST_JSON, jsonArray.toString()).apply()
    }

    fun getAllChatMessages(): List<ChatMessage> {
        val jsonStr = databasePrefs.getString(KEY_MESSAGES_LIST_JSON, null) ?: return emptyList()
        val list = mutableListOf<ChatMessage>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.optString("id"),
                        senderId = obj.optString("senderId"),
                        senderName = obj.optString("senderName"),
                        receiverId = obj.optString("receiverId"),
                        receiverName = obj.optString("receiverName"),
                        ciphertext = obj.optString("ciphertext"),
                        plaintext = obj.optString("plaintext"),
                        coeffA = obj.optInt("coeffA", 3),
                        coeffB = obj.optInt("coeffB", 2),
                        coeffC = obj.optInt("coeffC", 2),
                        coeffD = obj.optInt("coeffD", -1),
                        dateTime = obj.optString("dateTime"),
                        timestamp = obj.optLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getConversation(user1Id: String, user2Id: String): List<ChatMessage> {
        return getAllChatMessages().filter {
            (it.senderId == user1Id && it.receiverId == user2Id) ||
                    (it.senderId == user2Id && it.receiverId == user1Id)
        }.sortedBy { it.timestamp }
    }

    fun getRecentConversations(currentUserId: String): List<ChatMessage> {
        val allMessages = getAllChatMessages().filter {
            it.senderId == currentUserId || it.receiverId == currentUserId
        }.sortedByDescending { it.timestamp }

        val recentMap = mutableMapOf<String, ChatMessage>()
        for (m in allMessages) {
            val otherUserId = if (m.senderId == currentUserId) m.receiverId else m.senderId
            if (!recentMap.containsKey(otherUserId)) {
                recentMap[otherUserId] = m
            }
        }
        return recentMap.values.sortedByDescending { it.timestamp }
    }
}
