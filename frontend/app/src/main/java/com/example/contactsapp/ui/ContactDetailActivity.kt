package com.example.contactsapp.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.contactsapp.R
import com.example.contactsapp.databinding.ActivityContactDetailBinding
import com.example.contactsapp.model.Contact
import com.example.contactsapp.viewmodel.ContactListViewModel
import com.example.contactsapp.utils.UrlUtils

class ContactDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactDetailBinding
    private lateinit var viewModel: ContactListViewModel
    private val editContactRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ContactListViewModel::class.java)

        val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
        if (contactId != null) {
            fetchContactDetails(contactId)
        } else {
            finish()
        }

        setupButtons()
        observeViewModel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == editContactRequestCode && resultCode == RESULT_OK) {
            val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
            contactId?.let { fetchContactDetails(it) }
        }

    }
        private fun setupButtons() {
        binding.btnAddNewContact.setOnClickListener {
            val intent = Intent(this, AddEditContactActivity::class.java)
            startActivityForResult(intent, editContactRequestCode)
        }

        binding.btnBackToList.setOnClickListener {
            finish()
        }

        binding.btnEditContact.setOnClickListener {
            val contactId = viewModel.selectedContact.value?.id
            if (contactId != null) {
                val intent = Intent(this, AddEditContactActivity::class.java)
                intent.putExtra(EXTRA_CONTACT_ID, contactId)
                startActivityForResult(intent, editContactRequestCode)
            }
        }

        binding.btnCloseApp.setOnClickListener {
            finishAffinity()
        }
    }

    private fun fetchContactDetails(contactId: String) {
        viewModel.getContact(contactId)
    }

    private fun displayContactDetails(contact: Contact) {
        Log.d("ContactDetail", "Displaying contact: ${contact.name}")
        binding.apply {
            contactName.text = contact.name
            contactEmail.text = contact.email
            contactPhone.text = contact.phone
            contactAddress.text = contact.address ?: "No address provided"
            contactTitle.text = contact.title ?: "No title provided"
            contactStatus.text = contact.status ?: "No status provided"

            val imageUrl = contact.photoUrl?.let { UrlUtils.getImageUrl(it) }
            Log.d("ContactDetail", "Image URL: $imageUrl")

            Glide.with(this@ContactDetailActivity)
                .load(imageUrl ?: R.drawable.placeholder_image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(contactImage)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun observeViewModel() {
        viewModel.selectedContact.observe(this) { contact ->
            contact?.let { displayContactDetails(it) }
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let { showError(it) }
        }
    }

    companion object {
        const val EXTRA_CONTACT_ID = "extra_contact_id"
    }
}