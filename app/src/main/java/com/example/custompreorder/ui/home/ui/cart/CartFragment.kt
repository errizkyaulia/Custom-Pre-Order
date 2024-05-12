package com.example.custompreorder.ui.home.ui.cart

import CartAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.custompreorder.databinding.FragmentCartBinding

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartViewModel: CartViewModel
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inisialisasi CartViewModel
        cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)

        // Inisialisasi CartAdapter dengan daftar produk awal kosong
        cartAdapter = CartAdapter(ArrayList())

        // Atur RecyclerView dengan LinearLayoutManager dan adapter yang telah dibuat
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }

        // Observasi perubahan pada cartItems dan perbarui adapter ketika diperlukan
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            cartItems?.let {
                cartAdapter.updateCartList(it)
                // Setelah memperbarui adapter, hentikan animasi penyegaran
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Atur SwipeRefreshLayout dan listener-nya untuk "pull-to-refresh"
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Panggil metode untuk menyegarkan data dari view model
            cartViewModel.refreshCart()
            // jika cart kosong, tampilkan pesan "Cart is empty"
            if (cartViewModel.cartItems.value.isNullOrEmpty()) {
                binding.emptyCartTextView.visibility = View.VISIBLE
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}