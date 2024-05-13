package com.example.custompreorder.ui.dashboard

import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.custompreorder.data.OrderItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _orderList = MutableLiveData<List<OrderItem>>()
    val orderList: LiveData<List<OrderItem>> get() = _orderList

    init {
        // Load product list when ViewModel is initialized
        loadOrderList()
    }

    private fun loadOrderList() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()
        val ordersRef = db.collection("transactions")

        ordersRef.whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Log untuk setiap documents yang ditemukan
                val orderItemList = mutableListOf<OrderItem>()
                for (document in documents) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                    val orderItem = document.toObject(OrderItem::class.java)
                        .copy(orderId = document.id)
                    orderItemList.add(orderItem)
                }
                _orderList.value = orderItemList
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
                // Handle error here
            }
    }

    fun refreshOrderList() {
        loadOrderList()
    }

    companion object {
        private const val TAG = "DashboardViewModel"
    }
}