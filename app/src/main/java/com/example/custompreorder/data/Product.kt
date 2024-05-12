package com.example.custompreorder.data

data class Product(val id: String, val name: String, val description: String, val available: Boolean, val imageUrl: String) {
    fun isAvailable(): Boolean {
        return available
    }
}
