package com.example.easydialer.data

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydialer.R

class ListAdapter : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
    private var contactList = emptyList<Contact>()

    class MyViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_layout, parent, false))
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currItem = contactList[position]
        val contactUri = Uri.parse(currItem.uri)

        val cursor = holder.itemView.context.contentResolver.query(contactUri, arrayOf(ContactsContract.Contacts.PHOTO_URI, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
        val img = holder.itemView.findViewById<ImageView>(R.id.imgPhoto)
        val context = holder.itemView.context

        try {
            if (cursor == null || cursor.count == 0) return

            cursor.moveToFirst()
            val photoUri = cursor.getString(0)
            if (photoUri != null)
                Glide.with(holder.itemView.context).load(photoUri).into(img)

            val contactNumber = cursor.getString(1)

            img.setOnClickListener{
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$contactNumber")
                startActivity(context, callIntent, null)
            }
        }
        finally {
            cursor?.close()
        }
    }

    fun setData(contacts : List<Contact>) {
        this.contactList = contacts
        notifyDataSetChanged()
    }
}