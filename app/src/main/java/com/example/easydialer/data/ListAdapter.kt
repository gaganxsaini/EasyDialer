package com.example.easydialer.data

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.R
import java.io.File


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
        val img = holder.itemView.findViewById<ImageView>(R.id.imgPhoto)

        val context = holder.itemView.context
        val dir = context.filesDir.absolutePath
        val imgFile = File(dir + "//" + currItem.imagePath)
        if (imgFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
            img.setImageBitmap(myBitmap)
        }

        img.setOnClickListener{
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:" + currItem.phone)
            startActivity(context, callIntent, null)
        }
    }

    fun setData(contacts : List<Contact>) {
        this.contactList = contacts
        notifyDataSetChanged()
    }
}