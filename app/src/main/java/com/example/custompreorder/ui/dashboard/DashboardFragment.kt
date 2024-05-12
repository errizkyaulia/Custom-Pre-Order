package com.example.custompreorder.ui.dashboard

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.custompreorder.data.OrderAdapter
import com.example.custompreorder.data.ProductAdapter
import com.example.custompreorder.databinding.FragmentDashboardBinding
import com.example.custompreorder.databinding.FragmentHomeBinding
import com.example.custompreorder.ui.home.Cart
import com.example.custompreorder.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.ArrayList

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        orderAdapter = OrderAdapter(requireContext(), ArrayList())

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }

        dashboardViewModel.orderList.observe(viewLifecycleOwner) { orderList ->
            orderList?.let {
                orderAdapter.updateOrderList(it)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            dashboardViewModel.refreshOrderList()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}