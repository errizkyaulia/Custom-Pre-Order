import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.custompreorder.R
import com.example.custompreorder.data.CartItem
import com.example.custompreorder.ui.home.CheckoutOrder
import com.google.firebase.firestore.FirebaseFirestore

class CartAdapter(private var cartItems: List<CartItem>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val currentItem = cartItems[position]

        val productId = currentItem.product_id
        val itemSize = "Size: ${currentItem.size}"

        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val imgUrl = currentItem.designURL
                    val productName = document.get("name") as? String
                    // LOAD DOCUMENT PRODUCT
                    if (imgUrl != "") {
                        Glide.with(holder.itemView.context).load(imgUrl).into(holder.productImageView)
                        val cosName = "$productName With Custom Design"
                        holder.productNameTextView.text = cosName
                    } else {
                        val defaultImage = document.get("imageProductUrl") as? String
                        if (defaultImage != null) {
                            Glide.with(holder.itemView.context).load(defaultImage).into(holder.productImageView)
                            holder.productNameTextView.text = productName
                        } else {
                            holder.productImageView.setImageResource(R.drawable.baseline_broken_image_24)
                            holder.productNameTextView.text = productName
                        }
                    }

                    // LOAD PRICE per SIZE
                    val sizes = document.get("sizes") as? Map<String, Map<String, Any>>
                    sizes?.let { sizeMap ->
                        val selectedSize = sizeMap[currentItem.size]
                        if (selectedSize != null) {
                            val availability = selectedSize["availability"] as? Boolean
                            if (availability == true) {
                                // Load ALL CART DATA
                                val price = selectedSize["price"] as? Double
                                val priceView = "Rp. $price"
                                holder.productPriceTextView.text = priceView
                                holder.sizesTextView.text = itemSize
                                val itemQty = "${currentItem.quantity} Qty"
                                holder.qtyTextView.text = itemQty
                                val totalPrice = currentItem.quantity * price!!
                                val totalPriceView = "Rp. $totalPrice"
                                holder.totalPriceTextView.text = totalPriceView
                            } else {
                                Toast.makeText(holder.itemView.context, "Size ${currentItem.size} not available", Toast.LENGTH_SHORT).show()
                                Log.d("PriceInfo", "Size ${currentItem.size} not available")
                                holder.productPriceTextView.text = "N/A"
                                holder.totalPriceTextView.text = "N/A"
                                // Disable onClickListeners for this size
                                holder.itemView.setOnClickListener(null)
                            }
                        } else {
                            Toast.makeText(holder.itemView.context, "Price for Size ${currentItem.size} not found", Toast.LENGTH_SHORT).show()
                            Log.d("PriceInfo", "Size ${currentItem.size} not found")
                            holder.productPriceTextView.text = "N/A"
                            holder.totalPriceTextView.text = "N/A"
                            // Disable onClickListeners for this size
                            holder.itemView.setOnClickListener(null)
                        }
                    }
                } else {
                    holder.productImageView.setImageResource(R.drawable.baseline_broken_image_24)
                    holder.productNameTextView.text = "N/A"
                    holder.productPriceTextView.text = "N/A"
                    holder.totalPriceTextView.text = "N/A"
                    Toast.makeText(holder.itemView.context, "Product not found", Toast.LENGTH_SHORT).show()
                    Log.d("PriceInfo", "Product not found")
                    // Disable onClickListeners for this product
                    holder.itemView.setOnClickListener(null)
                }
            }

        // Tambahkan onClickListeners untuk setiap item
        holder.itemView.setOnClickListener {
            // Handle item click here
            val intent = Intent(holder.itemView.context, CheckoutOrder::class.java)
            // Tambahkan data documentID ke intent
            intent.putExtra("documentId", currentItem.documentId)
            intent.putExtra("productId", productId)
            intent.putExtra("name", holder.productNameTextView.text.toString())
            intent.putExtra("size", currentItem.size)
            intent.putExtra("quantity", currentItem.quantity.toString())
            intent.putExtra("price", holder.productPriceTextView.text.toString())
            intent.putExtra("totalPrice", holder.totalPriceTextView.text.toString())

            val cusUrl = currentItem.designURL
            if (cusUrl != "") {
                intent.putExtra("customDesignUrl", cusUrl)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    // Tambahkan metode untuk memperbarui daftar item keranjang
    fun updateCartList(newCartItems: List<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageView: ImageView = itemView.findViewById(R.id.productImageData)
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameData)
        val sizesTextView: TextView = itemView.findViewById(R.id.Sizes)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceData)
        val qtyTextView: TextView = itemView.findViewById(R.id.Qty)
        val totalPriceTextView: TextView = itemView.findViewById(R.id.totalPrice)
    }
}