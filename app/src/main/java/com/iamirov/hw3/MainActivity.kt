package com.iamirov.hw3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item.view.*


const val PERMISSION_CODE = 1111

data class Contact(val name: String, val phoneNumber: String)

class ContactViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
    val contactName: TextView = root.name
    val contactPhoneNumber: TextView = root.phoneNumber
}

class ContactAdapter(
    private val contacts: List<Contact>,
    val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactViewHolder {
        val holder = ContactViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item,
                parent,
                false
            )
        )
        holder.root.setOnClickListener {
            onClick(contacts[holder.adapterPosition])
        }
        return holder
    }


    override fun getItemCount(): Int = contacts.size

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.contactName.text = contacts[position].name
        holder.contactPhoneNumber.text = contacts[position].phoneNumber
    }

}


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var data: MutableList<Contact> = mutableListOf()
    private lateinit var permission: ContactsPermission

    private fun Context.fetchAllContacts(): List<Contact> {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
            .use { cursor ->
                if (cursor == null) return emptyList()
                val builder = ArrayList<Contact>()
                while (cursor.moveToNext()) {
                    val name =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                            ?: "N/A"
                    val phoneNumber =
                        cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            ?: "N/A"

                    builder.add(Contact(name, phoneNumber))
                }
                return builder
            }
    }

    class ContactsPermission {
        fun checkPermission(context: Context): Boolean {
            return (ContextCompat.checkSelfPermission(
                context as Activity,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED)
        }

        fun requestPermission(context: Context) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                PERMISSION_CODE
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permission = ContactsPermission()
        if (permission.checkPermission(this@MainActivity)) {
            permission.requestPermission(this@MainActivity)
        }
        setContentView(R.layout.activity_main)
        viewManager = LinearLayoutManager(this@MainActivity)
        viewAdapter = ContactAdapter(data) {
            startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${it.phoneNumber}")
                )
            )
        }
        pushData()
        recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

    }

    fun pushData() {
        data.clear()
        data.addAll(
            if (!permission.checkPermission(this@MainActivity)) {

                fetchAllContacts()
            } else
                listOf()
        )
        viewAdapter.notifyDataSetChanged()
        val contactsNum = resources.getQuantityString(R.plurals.numberOfContacts, data.size, data.size)
        Toast.makeText(this, "Found ${contactsNum}", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        pushData()

    }

    override fun onResume() {
        super.onResume()
        pushData()
    }


}
