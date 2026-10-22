package com.smktelkom.mathchat3.utils

import android.content.Context
import android.content.SharedPreferences
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.SavedContact
import com.smktelkom.mathchat3.models.User
import org.json.JSONArray
import org.json.JSONObject

class PreferenceManager(context: Context) {

    private val sessionPrefs: SharedPreferences =
        context.getSharedPreferences(PREF_SESSION_NAME, Context.MODE_PRIVATE)

    private val databasePrefs: SharedPreferences =
        context.getSharedPreferences(PREF_DATABASE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_SESSION_NAME = "ChatMathSession"
        private const val PREF_DATABASE_NAME = "ChatMathDatabase"

        // Session Keys
        const val KEY_IS_SIGNED_IN = "isSignedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_EMAIL = "email"
        const val KEY_IMAGE = "image"

        // Permanent Database Keys
        private const val KEY_USERS_LIST_JSON = "registered_users_json"
        private const val KEY_MESSAGES_LIST_JSON = "messages_list_json"
        private const val KEY_SAVED_CONTACTS_PREFIX = "saved_contacts_"
    }

    init {
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
                phoneNumber = "0811111111",
                email = "siswa1@gmail.com",
                password = "123456",
                imageBase64 = AvatarGenerator.generateAvatarBase64("Siswa Telkom 1")
            )
            val demo2 = User(
                id = "demo_siswa2",
                name = "Siswa Telkom 2",
                phoneNumber = "0822222222",
                email = "siswa2@gmail.com",
                password = "123456",
                imageBase64 = AvatarGenerator.generateAvatarBase64("Siswa Telkom 2")
            )
            val demo3 = User(
                id = "demo_guru",
                name = "Guru Matematika",
                phoneNumber = "0833333333",
                email = "guru.mtk@telkom.sch.id",
                password = "123456",
                imageBase64 = AvatarGenerator.generateAvatarBase64("Guru Matematika")
            )
            saveUser(demo1)
            saveUser(demo2)
            saveUser(demo3)
            FirebaseHelper.saveUser(demo1)
            FirebaseHelper.saveUser(demo2)
            FirebaseHelper.saveUser(demo3)
        }
    }

    fun saveUser(user: User) {
        val users = getAllUsers().toMutableList()
        users.removeAll {
            it.id == user.id ||
                    it.email.equals(user.email, ignoreCase = true) ||
                    (user.phoneNumber.isNotEmpty() && it.phoneNumber == user.phoneNumber)
        }
        users.add(user)

        val jsonArray = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("phoneNumber", u.phoneNumber)
                put("email", u.email)
                put("password", u.password)
                put("imageBase64", u.imageBase64)
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
                        phoneNumber = obj.optString("phoneNumber"),
                        email = obj.optString("email"),
                        password = obj.optString("password"),
                        imageBase64 = obj.optString("imageBase64")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun findUserByEmailOrPhone(query: String): User? {
        val cleanQuery = query.trim()
        return getAllUsers().firstOrNull {
            it.email.equals(cleanQuery, ignoreCase = true) ||
                    (it.phoneNumber.isNotEmpty() && it.phoneNumber == cleanQuery)
        }
    }

    fun findUserByEmail(email: String): User? {
        return findUserByEmailOrPhone(email)
    }

    fun getCurrentUser(): User? {
        val id = getString(KEY_USER_ID) ?: return null
        val name = getString(KEY_NAME) ?: ""
        val phone = getString(KEY_PHONE) ?: ""
        val email = getString(KEY_EMAIL) ?: ""
        val image = getString(KEY_IMAGE) ?: ""
        return User(id = id, name = name, phoneNumber = phone, email = email, imageBase64 = image)
    }

    // =========================================================================
    // MANAGEMENT SAVED CONTACTS (ChatMathDatabase)
    // =========================================================================

    fun saveContact(currentUserId: String, contact: SavedContact) {
        val contacts = getSavedContacts(currentUserId).toMutableList()
        contacts.removeAll { it.contactUserId == contact.contactUserId }
        contacts.add(contact)

        val jsonArray = JSONArray()
        for (c in contacts) {
            val obj = JSONObject().apply {
                put("ownerUserId", c.ownerUserId)
                put("contactUserId", c.contactUserId)
                put("customAliasName", c.customAliasName)
                put("phoneNumber", c.phoneNumber)
                put("email", c.email)
                put("imageBase64", c.imageBase64)
                put("isFavorite", c.isFavorite)
            }
            jsonArray.put(obj)
        }
        databasePrefs.edit().putString(KEY_SAVED_CONTACTS_PREFIX + currentUserId, jsonArray.toString()).apply()
    }

    fun getSavedContacts(currentUserId: String): List<SavedContact> {
        val jsonStr = databasePrefs.getString(KEY_SAVED_CONTACTS_PREFIX + currentUserId, null) ?: return emptyList()
        val list = mutableListOf<SavedContact>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    SavedContact(
                        ownerUserId = obj.optString("ownerUserId"),
                        contactUserId = obj.optString("contactUserId"),
                        customAliasName = obj.optString("customAliasName"),
                        phoneNumber = obj.optString("phoneNumber"),
                        email = obj.optString("email"),
                        imageBase64 = obj.optString("imageBase64"),
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Urutkan otomatis: isFavorite == true selalu tampil paling atas!
        return list.sortedWith(compareByDescending<SavedContact> { it.isFavorite }.thenBy { it.customAliasName })
    }

    fun toggleContactFavorite(currentUserId: String, contactUserId: String): Boolean {
        val contacts = getSavedContacts(currentUserId).toMutableList()
        val index = contacts.indexOfFirst { it.contactUserId == contactUserId }
        if (index != -1) {
            val updated = contacts[index].copy(isFavorite = !contacts[index].isFavorite)
            saveContact(currentUserId, updated)
            return updated.isFavorite
        }
        return false
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
