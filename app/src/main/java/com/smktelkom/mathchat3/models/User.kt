package com.smktelkom.mathchat3.models

import java.io.Serializable

data class User(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val password: String = "",
    val imageBase64: String = ""
) : Serializable
