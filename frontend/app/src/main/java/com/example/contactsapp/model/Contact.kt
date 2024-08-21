package com.example.contactsapp.model


data class Contact(
    val id: String?,
    val name: String,
    val title: String?,
    val email: String,
    val phone: String,
    val address: String?,
    val status: String?,
    var photoUrl: String?
)