package com.example.custompreorder.data

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.custompreorder.R
import com.google.firebase.firestore.FirebaseFirestore

class OrderAdapter(private val mContext: Context, private var mOrderList: List<OrderItem>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val orderItem = mOrderList[position]

        // Set data to views
        val holderQty = "${orderItem.quantity} Qty"
        holder.qty.text = holderQty
        holder.productName.text = orderItem.product_name // Using directly from OrderItem

        val holderSizes = "Size: ${orderItem.size}"
        holder.sizes.text = holderSizes

        val priceVal = "Rp. " + orderItem.product_price
        holder.price.text = priceVal
        val totalPriceVal = "Rp. " + orderItem.total_price
        holder.totalPrice.text = totalPriceVal
        holder.status.text = orderItem.status
        holder.status.setTextColor(
            when (orderItem.status) {
                "Ordered" -> {
                    mContext.resources.getColor(R.color.blue)
                }
                "accepted" -> {
                    mContext.resources.getColor(R.color.green)
                }
                "Done" -> {
                    mContext.resources.getColor(R.color.black)
                }
                else -> {
                    mContext.resources.getColor(R.color.red)
                }
            }
        )

        val imgUrl = orderItem.designUrl
        // Load image from designUrl if not empty, otherwise load default image
        if (imgUrl != "") {
            // Log img url to check
            Log.d(ContentValues.TAG, imgUrl)
            Glide.with(mContext)
                .load(imgUrl)
                .into(holder.productImage)
        } else {
            // Load default image from firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("products")
                .document(orderItem.product_id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val defaultDesignUrl = document.getString("imageProductUrl")
                            if (defaultDesignUrl != null) {
                                Glide.with(mContext)
                                    .load(defaultDesignUrl)
                                    .into(holder.productImage)
                            }
                        }
                    }
        }

        // Set click listener to handle item click
        holder.itemView.setOnClickListener {
            // Handle item click here if needed
        }
    }

    override fun getItemCount(): Int {
        return mOrderList.size
    }

    // Method to update the list of orders in the adapter
    fun updateOrderList(orderList: List<OrderItem>) {
        mOrderList = orderList
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var productImage: ImageView = itemView.findViewById(R.id.productImageData)
        var productName: TextView = itemView.findViewById(R.id.productNameData)
        var qty: TextView = itemView.findViewById(R.id.Qty)
        var sizes: TextView = itemView.findViewById(R.id.Sizes)
        var price: TextView = itemView.findViewById(R.id.productPriceData)
        var totalPrice: TextView = itemView.findViewById(R.id.totalPrice)
        var status: TextView = itemView.findViewById(R.id.status)

    }
}