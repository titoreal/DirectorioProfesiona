package com.example.contactsapp.network

import com.example.contactsapp.model.Contact
import com.example.contactsapp.model.PaginatedResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface ApiService {
    @Multipart
    @PUT("contacts/photo")
    suspend fun uploadPhoto(
        @Part("id") id: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("contacts")
    suspend fun getContacts(@Query("page") page: Int, @Query("size") size: Int): Response<PaginatedResponse<Contact>>

    @GET("contacts/{id}")
    suspend fun getContact(@Path("id") id: String): Response<Contact>

    @POST("contacts")
    suspend fun saveContact(@Body contact: Contact): Response<Contact>

    @POST("contacts")
    suspend fun updateContact(@Body contact: Contact): Response<Contact>

    @PUT("contacts/photo")
    suspend fun updatePhoto(@Body formData: MultipartBody.Part): Response<ResponseBody>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: Int): Response<ResponseBody>
}
