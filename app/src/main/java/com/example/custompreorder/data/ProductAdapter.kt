package com.example.custompreorder.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.custompreorder.R
import com.example.custompreorder.ui.home.CustomizeOrder


class ProductAdapter(private val mContext: Context, private var mProductList: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = mProductList[position]

        // Set data to views
        holder.productName.text = product.name
        holder.availability.text = if (product.isAvailable()) "Available" else "Not Available"
        // Set color based on availability
        holder.availability.setTextColor(
            if (product.isAvailable()) Color.GREEN else Color.RED
        )
        Glide.with(mContext).load(product.imageUrl).into(holder.productImage)

        // Set click listener to handle item click
        holder.itemView.setOnClickListener {

            // Jika produk tidak tersedia, tampilkan pesan
            if (!product.isAvailable()) {
                Toast.makeText(mContext, "Product not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create an Intent to navigate to CustimizeOrder activity
            val intent = Intent(mContext, CustomizeOrder::class.java)
            // Put the product ID as an extra to the Intent
            intent.putExtra("productId", product.id)
            // Start the activity
            mContext.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return mProductList.size
    }

    // Method to update the list of products in the adapter
    fun updateProductList(productList: List<Product>) {
        mProductList = productList
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var productImage: ImageView = itemView.findViewById(R.id.productImageData)
        var productName: TextView = itemView.findViewById(R.id.productNameData)
        var availability: TextView = itemView.findViewById(R.id.Availability)
        var cardView: CardView = itemView.findViewById<CardView>(R.id.productCard)
    }
}
