package com.example.custompreorder.data

import com.google.firebase.Timestamp

data class CartItem(
    val documentId: String = "", // Menambahkan properti documentId
    val date: Timestamp = Timestamp.now(),
    val user_id: String = "",
    val product_id: String = "",
    val size: String = "",
    val quantity: Int = 0,
    val designURL: String = "",
)