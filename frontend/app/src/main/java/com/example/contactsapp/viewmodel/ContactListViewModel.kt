package com.example.contactsapp.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.model.Contact
import com.example.contactsapp.network.ApiClient
import com.example.contactsapp.network.ApiClient.apiService
import com.example.contactsapp.utils.UrlUtils
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class ContactListViewModel(application: Application) : AndroidViewModel(application) {
    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    private val _networkError = MutableLiveData<Boolean>()
    val networkError: LiveData<Boolean> = _networkError

    private val _contactOperationResult = MutableLiveData<Result<Contact>>()
    val contactOperationResult: LiveData<Result<Contact>> = _contactOperationResult

    private val _selectedContact = MutableLiveData<Contact>()
    val selectedContact: LiveData<Contact> = _selectedContact

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentPage = 0
    private val pageSize = 20

    fun loadContacts() {
        viewModelScope.launch {
            if (isNetworkAvailable(getApplication())) {
                currentPage = 0  // Resetear la página a 0 para obtener los datos más recientes
                fetchContacts()
            } else {
                _networkError.value = true
                Log.e("Network Error", "No network connection available")
            }
        }
    }

    private suspend fun fetchContacts() {
        try {
            val response = ApiClient.apiService.getContacts(currentPage, pageSize)
            if (response.isSuccessful) {
                val paginatedResponse = response.body()
                if (paginatedResponse != null) {
                    val contacts = paginatedResponse.content.map { contact ->
                        contact.copy(photoUrl = UrlUtils.getImageUrl(contact.photoUrl ?: ""))
                    }
                    _contacts.postValue(contacts)
                    Log.d("ViewModel", "Contacts fetched: ${contacts.size}")
                } else {
                    Log.e("API Error", "Response body is null")
                }
            } else {
                Log.e("API Error", "Response not successful: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("API Exception", "Error fetching contacts", e)
        }
    }

    fun getContact(id: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getContact(id)
                if (response.isSuccessful) {
                    _selectedContact.value = response.body()
                } else {
                    _error.value = "Failed to fetch contact details"
                }
            } catch (e: Exception) {
                _error.value = "An error occurred: ${e.message}"
            }
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.updateContact(contact)
                if (response.isSuccessful) {
                    _contactOperationResult.value = Result.success(response.body()!!)
                } else {
                    _contactOperationResult.value =
                        Result.failure(Exception("Failed to update contact"))
                }
            } catch (e: Exception) {
                _contactOperationResult.value = Result.failure(e)
            }
        }
    }

    fun createNewContact(contact: Contact) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.saveContact(contact)
                if (response.isSuccessful) {
                    _contactOperationResult.value = Result.success(response.body()!!)
                } else {
                    _contactOperationResult.value =
                        Result.failure(Exception("Failed to create contact"))
                }
            } catch (e: Exception) {
                _contactOperationResult.value = Result.failure(e)
            }
        }
    }

    suspend fun uploadPhoto(contactId: String, photo: MultipartBody.Part): Result<String> {
        return try {
            val response = apiService.uploadPhoto(contactId.toRequestBody("text/plain".toMediaTypeOrNull()), photo)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""
                Result.success(responseBody)
            } else {
                Result.failure(IOException("Error en la respuesta del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
