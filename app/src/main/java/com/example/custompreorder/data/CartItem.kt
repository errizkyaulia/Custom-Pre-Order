package com.example.custompreorder.data

data class CartItem(
    val productId: String,
    val size: String,
    val quantity: Int,
    val designURL: String,
    val productName: String,
    val price: Double
)