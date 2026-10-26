package com.smktelkom.mathchat3.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smktelkom.mathchat3.models.ChatMessage
import com.smktelkom.mathchat3.models.User

object FirebaseHelper {

    private const val TAG = "ChatMathFirebase"
    private const val PATH_USERS = "users"
    private const val PATH_CHATS = "chats"

    private val notifiedMessageIds = mutableSetOf<String>()
    private var isInitialLoad = true

    var currentActiveChatReceiverId: String? = null

    private const val DB_URL_ASIA = "https://chatmathdb-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val DB_URL_US = "https://chatmathdb-default-rtdb.firebaseio.com"

    private val database: FirebaseDatabase by lazy {
        val db = try {
            FirebaseDatabase.getInstance(DB_URL_ASIA)
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance(DB_URL_US)
            } catch (e2: Exception) {
                FirebaseDatabase.getInstance()
            }
        }
        try {
            db.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Ignored if persistence already enabled
        }
        db
    }

    private fun showToastOnUI(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun saveUser(user: User) {
        try {
            database.reference.child(PATH_USERS).child(user.id).setValue(user)
                .addOnSuccessListener {
                    Log.d(TAG, "User berhasil disimpan di Firebase: ${user.name}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gagal menyimpan user di Firebase: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat saveUser: ${e.message}", e)
        }
    }

    fun saveChatMessage(message: ChatMessage) {
        try {
            database.reference.child(PATH_CHATS).child(message.id).setValue(message)
                .addOnSuccessListener {
                    Log.d(TAG, "Pesan berhasil dikirim ke Firebase: ${message.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gagal mengirim pesan ke Firebase: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saat saveChatMessage: ${e.message}", e)
        }
    }

    fun listenForUsers(
        context: Context,
        preferenceManager: PreferenceManager,
        onUsersUpdated: (List<User>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<User>()
                for (child in snapshot.children) {
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        userList.add(user)
                        preferenceManager.saveUser(user)
                    }
                }
                Log.d(TAG, "Realtime Users updated: total ${userList.size} users")
                onUsersUpdated(userList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listenForUsers Cancelled/Error: ${error.message} (Code: ${error.code})")
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showToastOnUI(context, "Firebase Error: Permission Denied! Ubah Rules di Firebase Console ke true.")
                }
                onUsersUpdated(preferenceManager.getAllUsers())
            }
        }
        database.reference.child(PATH_USERS).addValueEventListener(listener)
        return listener
    }

    fun listenForConversation(
        context: Context,
        currentUserId: String,
        receiverId: String,
        preferenceManager: PreferenceManager,
        onMessagesUpdated: (List<ChatMessage>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversation = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    if (message != null) {
                        preferenceManager.saveChatMessage(message)
                        if ((message.senderId == currentUserId && message.receiverId == receiverId) ||
                            (message.senderId == receiverId && message.receiverId == currentUserId)
                        ) {
                            conversation.add(message)
                        }
                        checkAndShowNotification(context, currentUserId, message, preferenceManager)
                    }
                }
                isInitialLoad = false
                conversation.sortBy { it.timestamp }
                onMessagesUpdated(conversation)
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showToastOnUI(context, "Firebase Error: Permission Denied! Ubah Rules di Firebase Console ke true.")
                }
                onMessagesUpdated(preferenceManager.getConversation(currentUserId, receiverId))
            }
        }
        database.reference.child(PATH_CHATS).addValueEventListener(listener)
        return listener
    }

    fun listenForRecentConversations(
        context: Context,
        currentUserId: String,
        preferenceManager: PreferenceManager,
        onRecentUpdated: (List<ChatMessage>) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    if (message != null) {
                        preferenceManager.saveChatMessage(message)
                        checkAndShowNotification(context, currentUserId, message, preferenceManager)
                    }
                }
                isInitialLoad = false
                val recent = preferenceManager.getRecentConversations(currentUserId)
                onRecentUpdated(recent)
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    showToastOnUI(context, "Firebase Error: Permission Denied! Ubah Rules di Firebase Console ke true.")
                }
                onRecentUpdated(preferenceManager.getRecentConversations(currentUserId))
            }
        }
        database.reference.child(PATH_CHATS).addValueEventListener(listener)
        return listener
    }

    private fun checkAndShowNotification(
        context: Context,
        currentUserId: String,
        message: ChatMessage,
        preferenceManager: PreferenceManager
    ) {
        if (message.receiverId == currentUserId && message.senderId != currentUserId) {
            if (!notifiedMessageIds.contains(message.id)) {
                notifiedMessageIds.add(message.id)
                if (!isInitialLoad) {
                    if (currentActiveChatReceiverId == message.senderId) {
                        return
                    }

                    val senderUser = preferenceManager.getAllUsers().firstOrNull { it.id == message.senderId }
                        ?: User(id = message.senderId, name = message.senderName)

                    NotificationHelper.showNewMessageNotification(
                        context,
                        senderId = message.senderId,
                        senderName = message.senderName,
                        plaintext = message.plaintext,
                        ciphertext = message.ciphertext,
                        senderUser = senderUser
                    )
                }
            }
        }
    }

    fun removeUsersListener(listener: ValueEventListener) {
        database.reference.child(PATH_USERS).removeEventListener(listener)
    }

    fun removeChatsListener(listener: ValueEventListener) {
        database.reference.child(PATH_CHATS).removeEventListener(listener)
    }
}
