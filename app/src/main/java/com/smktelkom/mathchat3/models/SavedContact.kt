package com.smktelkom.mathchat3.models

import java.io.Serializable

data class SavedContact(
    val ownerUserId: String = "",
    val contactUserId: String = "",
    val customAliasName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val imageBase64: String = "",
    var isFavorite: Boolean = false
) : Serializable
