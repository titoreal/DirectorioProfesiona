package com.example.contactsapp.utils

import com.example.contactsapp.network.ApiClient

object UrlUtils {
    fun getImageUrl(photoUrl: String): String {
        return when {
            photoUrl.startsWith("http://localhost:8080/") ->
                "${ApiClient.BASE_URL}${photoUrl.removePrefix("http://localhost:8080/")}"
            photoUrl.startsWith("http") -> photoUrl
            else -> "${ApiClient.BASE_URL}${photoUrl.removePrefix("/")}"
        }
    }
}