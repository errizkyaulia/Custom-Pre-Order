package com.example.custompreorder.ui.home.ui.cart

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.custompreorder.data.CartItem
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
        // Dapatkan User ID dari FirebaseAuth
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // LOG USER ID
        Log.d(TAG, "User ID: $userId")
        // Akses koleksi "cart" untuk pengguna tertentu di Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("cart")
            .document(userId)
            .collection("items")
            .get()
            .addOnSuccessListener { result ->
                // Log 1
                Log.d(TAG, "Cart items loaded: ${result.documents.size}")

                val cartList = mutableListOf<CartItem>()

                // LOG isi cartList
                Log.d(TAG, "Cart items: $cartList")
                for (document in result) {
                    val productId = document.id // Mengambil id produk
                    val orderMap = document.data // Mendapatkan map order
                    // Log 2
                    Log.d(TAG, "Order map for product $productId: $orderMap")
                    // Loop melalui map order untuk mendapatkan informasi produk
                    for ((size, orderData) in orderMap) {
                        val quantity = (orderData as Map<*, *>)["quantity"] as Long
                        val designURL = orderData["designURL"] as String

                        // Log 3
                        Log.d(TAG, "Product ID: $productId, Size: $size, Quantity: $quantity")

                        // Mengambil data produk dan mengupdate objek CartItem
                        fetchProductData(productId, size,
                            { productName, price ->
                                // Membuat objek CartItem untuk setiap produk
                                val cartItem = CartItem(productId, size, quantity.toInt(), designURL, productName, price)
                                cartList.add(cartItem)
                                // Log 4
                                Log.d(TAG, "Added cart item: $cartItem")
                            },
                            {
                                // Handle jika gagal mengambil data produk
                                Log.e(TAG, "Failed to fetch product data for product: $productId")
                            }
                        )
                    }
                }
                // Setelah selesai mengambil semua produk, perbarui LiveData
                _cartItems.value = cartList
            }
            .addOnFailureListener { exception ->
                // Handle any errors
                Log.e(TAG, "Error getting products documents: ", exception)
            }
    }

    private fun fetchProductData(productId: String, size: String, onSuccess: (String, Double) -> Unit, onFailure: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products").document(productId)

        productRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val productName = documentSnapshot.getString("productName")
                val priceMap = documentSnapshot.get("prices") as Map<String, Any>?
                val price = priceMap?.get(size) as? Double ?: 0.0
                if (productName != null && priceMap != null) {
                    onSuccess.invoke(productName, price)
                } else {
                    onFailure.invoke()
                    Log.e(TAG, "Missing product name or price data for product: $productId")
                }
            } else {
                onFailure.invoke()
                Log.e(TAG, "Product not found: $productId")
            }
        }.addOnFailureListener { exception ->
            onFailure.invoke()
            Log.e(TAG, "Failed to fetch product data for product: $productId", exception)
        }
    }

    fun refreshCart() {
        loadCartList()
    }

    companion object {
        private const val TAG = "CartViewModel"
    }
}