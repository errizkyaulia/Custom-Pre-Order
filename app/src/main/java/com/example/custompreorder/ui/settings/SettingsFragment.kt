package com.example.custompreorder.ui.settings

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.custompreorder.Login
import com.example.custompreorder.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val settingsViewModel =
            ViewModelProvider(this).get(SettingsViewModel::class.java)

        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLogoutConfirmationDialog() {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        alertDialogBuilder.setTitle("Logout")
        alertDialogBuilder.setMessage("Are you sure you want to log out?")
        alertDialogBuilder.setPositiveButton("Yes") { dialogInterface: DialogInterface, _: Int ->
            logout()
            dialogInterface.dismiss()
        }
        alertDialogBuilder.setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(requireContext(), Login::class.java)
        startActivity(intent)
        requireActivity().finish()
    }
}