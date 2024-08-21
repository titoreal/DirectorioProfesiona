package com.example.contactsapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactsapp.adapter.ContactAdapter
import com.example.contactsapp.databinding.ActivityContactListBinding
import com.example.contactsapp.viewmodel.ContactListViewModel


class ContactListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactListBinding
    private lateinit var viewModel: ContactListViewModel
    private lateinit var adapter: ContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(ContactListViewModel::class.java)

        setupViewModel()
        setupRecyclerView()
        observeViewModel()
        viewModel.loadContacts()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContacts()  // Recargar contactos cada vez que la actividad se reanude
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(ContactListViewModel::class.java)
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter { contact ->
            val intent = Intent(this, ContactDetailActivity::class.java)
            intent.putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id)
            startActivity(intent)
        }
        binding.contactsRecyclerview.apply {
            layoutManager = LinearLayoutManager(this@ContactListActivity)
            adapter = this@ContactListActivity.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.contacts.observe(this) { contacts ->
            Log.d("ContactListActivity", "Contacts received: ${contacts.size}")
            contacts.forEach { contact ->
                Log.d("ContactListActivity", "Contact: ${contact.name}, PhotoUrl: ${contact.photoUrl}")
            }
            adapter.submitList(contacts)
        }

        viewModel.networkError.observe(this) { hasError ->
            if (hasError) {
                Toast.makeText(this, "No hay conexión de red disponible", Toast.LENGTH_LONG).show()
            }
        }
    }

}