package com.example.custompreorder.ui.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.custompreorder.R
import com.example.custompreorder.databinding.ActivityCartBinding
import com.example.custompreorder.databinding.ActivityMenuBinding
import com.example.custompreorder.ui.home.ui.cart.CartFragment

class Cart : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the action bar name to Cart
        supportActionBar?.hide()
        // Tampilkan Fragment Cart
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, CartFragment())
            .commit()

    }
}