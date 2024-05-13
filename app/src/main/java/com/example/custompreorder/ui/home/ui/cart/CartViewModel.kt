package com.example.custompreorder.ui.home.ui.cart

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.custompreorder.data.CartItem
import com.example.custompreorder.data.OrderItem
import com.example.custompreorder.ui.dashboard.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CartViewModel : ViewModel() {
    private val _cartItems = MutableLiveData<List<CartItem>>()
    val cartItems: LiveData<List<CartItem>> get() = _cartItems

    init {
        // Load cart list when ViewModel is initialized
        loadCartList()
    }

    private fun loadCartList() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        val cartsRef = db.collection("cart")

        cartsRef.whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Log untuk setiap documents yang ditemukan
                val cartList = mutableListOf<CartItem>()
                for (document in documents) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                    val cartItems = document.toObject(CartItem::class.java)
                        .copy(documentId = document.id)
                    cartList.add(cartItems)
                }
                _cartItems.value = cartList
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
                // Handle error here
            }
    }

    fun refreshCart() {
        loadCartList()
    }

    companion object {
        private const val TAG = "CartViewModel"
    }
}