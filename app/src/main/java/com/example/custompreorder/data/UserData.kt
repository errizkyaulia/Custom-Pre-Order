package com.example.custompreorder.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class UserData {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Fungsi untuk mendapatkan data pengguna saat ini
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    // Fungsi untuk mendapatkan ID pengguna saat ini
    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    // Fungsi untuk mendapatkan ProfileFragment Picture dari Firestore
    fun getProfilePictureUrl(userId: String): String? {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId)
        var profilePicture: String? = null
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                profilePicture = document.getString("profilePictureUrl")
            }
        }
        return profilePicture
    }

    // Fungsi untuk mendapatkan Fullname dari Firestore
    fun getFullName(userId: String): String? {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId)
        var fullName: String? = null
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                fullName = document.getString("fullname")
            }
        }
        return fullName
    }

    // Fungsi untuk mendapatkan Address dari Firestore
    fun getAddress(userId: String): String? {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId)
        var address: String? = null
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                address = document.getString("address")
            }
            }
        return address
    }

    fun getPhoneNumber(userId: String): String? {
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId)
        var phoneNumber: String? = null
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                phoneNumber = document.getString("phonenumber")
            }
        }
        return phoneNumber
    }

}

