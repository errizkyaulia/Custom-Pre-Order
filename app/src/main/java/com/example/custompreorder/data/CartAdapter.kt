import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.custompreorder.R
import com.example.custompreorder.data.CartItem

class CartAdapter(private var cartItems: List<CartItem>) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productNameTextView: TextView = itemView.findViewById(R.id.productNameData)
        val sizesTextView: TextView = itemView.findViewById(R.id.Sizes)
        val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceData)
        val qtyTextView: TextView = itemView.findViewById(R.id.Qty)
        val totalPriceTextView: TextView = itemView.findViewById(R.id.totalPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val currentItem = cartItems[position]

        // Bind data to views
//        holder.productNameTextView.text = currentItem.productName
        holder.sizesTextView.text = currentItem.size
//        holder.productPriceTextView.text = currentItem.productPrice?.toString() ?: "N/A"
//        holder.qtyTextView.text = currentItem.quantity?.toString() ?: "N/A"

        // Calculate total price if both product price and quantity are not null
//        val totalPrice = currentItem.productPrice?.times(currentItem.quantity ?: 0)
//        holder.totalPriceTextView.text = totalPrice?.toString() ?: "N/A"
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    // Tambahkan metode untuk memperbarui daftar item keranjang
    fun updateCartList(newCartItems: List<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }
}