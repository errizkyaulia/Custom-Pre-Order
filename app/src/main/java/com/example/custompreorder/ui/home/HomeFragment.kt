package com.example.custompreorder.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.custompreorder.data.ProductAdapter
import com.example.custompreorder.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inisialisasi HomeViewModel
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Inisialisasi ProductAdapter dengan daftar produk awal kosong
        productAdapter = ProductAdapter(requireContext(), ArrayList())

        // Atur RecyclerView dengan LinearLayoutManager dan adapter yang telah dibuat
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }

        // Observasi perubahan pada productList dan perbarui adapter ketika diperlukan
        homeViewModel.productList.observe(viewLifecycleOwner) { productList ->
            productList?.let {
                productAdapter.updateProductList(it)
                // After updating the adapter, stop the refreshing animation
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        // Set up SwipeRefreshLayout and its listener for "pull-to-refresh"
        binding.swipeRefreshLayout.setOnRefreshListener {
            // Call the method to refresh data from the view model
            homeViewModel.refreshData()
        }

        binding.cartButton.setOnClickListener {
            // To cart Activity
            val intent = Intent(requireContext(), Cart::class.java)
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}