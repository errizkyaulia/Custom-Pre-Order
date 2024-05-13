package com.example.custompreorder.data

import com.google.firebase.Timestamp

data class OrderItem(
    val orderId: String = "",
    val user_id: String = "",
    val date: Timestamp = Timestamp.now(),
    val total_price: Double = 0.0,
    val product_id: String = "",
    val product_name: String = "",
    val product_price: Double = 0.0,
    val size: String = "",
    val quantity: Int = 0,
    val designUrl: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val status: String = ""
)

