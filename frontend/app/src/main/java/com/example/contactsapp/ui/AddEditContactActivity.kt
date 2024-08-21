package com.example.contactsapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.contactsapp.R
import com.example.contactsapp.databinding.ActivityAddEditContactBinding
import com.example.contactsapp.model.Contact
import com.example.contactsapp.viewmodel.ContactListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class AddEditContactActivity : AppCompatActivity() {
    companion object {
        private const val GALLERY_REQUEST_CODE = 1000
        private const val CAMERA_REQUEST_CODE = 1001
        const val EXTRA_CONTACT_ID = "extra_contact_id"
    }
    private lateinit var binding: ActivityAddEditContactBinding
    private lateinit var viewModel: ContactListViewModel
    private var selectedImageUri: Uri? = null
    private var isEditMode = false
    private var contactId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditContactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ContactListViewModel::class.java)
        contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
        isEditMode = contactId != null

        if (isEditMode) {
            loadContact()
        }

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener {
            showImageSourceDialog()
        }

        binding.btnSave.setOnClickListener {
            saveContact()
        }
    }
    private fun loadContact() {
        contactId?.let { id ->
            viewModel.getContact(id)
            viewModel.selectedContact.observe(this) { loadedContact: Contact? ->
                loadedContact?.let { contact ->
                    binding.etName.setText(contact.name)
                    binding.etTitle.setText(contact.title)
                    binding.etEmail.setText(contact.email)
                    binding.etPhone.setText(contact.phone)
                    binding.etAddress.setText(contact.address)
                    binding.etStatus.setText(contact.status)

                    Glide.with(this)
                        .load(contact.photoUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(binding.contactImage)
                }
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Cámara", "Galería")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar imagen desde")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap?
                    imageBitmap?.let {
                        selectedImageUri = saveImageToFile(it)
                        binding.contactImage.setImageBitmap(it)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    selectedImageUri = data?.data
                    binding.contactImage.setImageURI(selectedImageUri)
                }
            }
        }
    }

    private fun saveImageToFile(bitmap: Bitmap): Uri {
        val file = File(externalCacheDir, "temp_image.jpg")
        file.createNewFile()
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }

    private fun saveContact() {
        val name = binding.etName.text.toString()
        val title = binding.etTitle.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val address = binding.etAddress.text.toString()
        val status = binding.etStatus.text.toString()

        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            Toast.makeText(this, "Por favor, completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val contact = Contact(
            id = if (isEditMode) contactId else null,
            name = name,
            title = title,
            email = email,
            phone = phone,
            address = address,
            status = status,
            photoUrl = null
        )

        if (isEditMode) {
            viewModel.updateContact(contact)
        } else {
            viewModel.createNewContact(contact)
        }
        viewModel.contactOperationResult.observe(this) { result ->
            if (result.isSuccess) {
                val savedContact = result.getOrNull()
                savedContact?.let { contact ->
                    selectedImageUri?.let { uri ->
                        uploadPhoto(uri)
                    } ?: run {
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Error al ${if (isEditMode) "actualizar" else "crear"} el contacto", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun observeViewModel() {
        viewModel.contactOperationResult.observe(this) { result: Result<Contact> ->
            when {
                result.isSuccess -> {
                    result.getOrNull()?.let { contact ->
                        selectedImageUri?.let { uri ->
                            uploadPhoto(uri)
                        } ?: run {
                            finish()
                        }
                    }
                }
                result.isFailure -> {
                    Toast.makeText(this, "Error al ${if (isEditMode) "actualizar" else "crear"} el contacto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun uploadPhoto(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val contactId = viewModel.contactOperationResult.value?.getOrNull()?.id
                    ?: throw IllegalStateException("Contact ID is null")

                val contentResolver = applicationContext.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("Failed to open input stream for URI: $uri")

                val fileName = getFileNameFromUri(uri) ?: "photo.jpg"
                val requestFile = inputStream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)

                val result = viewModel.uploadPhoto(contactId, filePart)
                result.onSuccess { photoUrl ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddEditContactActivity, "Foto subida con éxito", Toast.LENGTH_SHORT).show()
                        // Aquí puedes hacer algo con la URL de la foto si lo necesitas
                        finish()
                    }
                }.onFailure { error ->
                    Log.e("AddEditContactActivity", "Error during photo upload", error)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddEditContactActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddEditContactActivity", "Exception during photo upload", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddEditContactActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
}






