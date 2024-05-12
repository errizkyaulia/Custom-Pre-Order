package com.example.custompreorder.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.custompreorder.data.Product
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _productList = MutableLiveData<List<Product>>()
    val productList: LiveData<List<Product>> get() = _productList

    init {
        // Load product list when ViewModel is initialized
        loadProductList()
    }

    private fun loadProductList() {
        // Access "products" collection in Firestore
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val productList = mutableListOf<Product>()
                for (document in result) {
                    val id = document.id // Mengambil id dokumen
                    val name = document.getString("name") ?: ""
                    val description = document.getString("description") ?: ""
                    val imageProductUrl = document.getString("imageProductUrl") ?: ""
                    val availabilityProduct = document.getBoolean("availabilityProduct") ?: false
                    val product = Product(id, name, description, availabilityProduct, imageProductUrl)
                    productList.add(product)
                }
                _productList.value = productList
            }
            .addOnFailureListener { exception ->
                // Handle any errors
                // Log the error message
                Log.e(TAG, "Error getting documents: ", exception)
                // Notify the user or perform any necessary action
            }
    }

    fun refreshData() {
        loadProductList()
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}