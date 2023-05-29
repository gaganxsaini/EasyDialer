package com.example.easydialer.data

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.R
import java.io.File


class ViewContactListAdapter : RecyclerView.Adapter<ViewContactListAdapter.MyViewHolder>() {
        private var contactList = mutableListOf<Contact>()

        inner class MyViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView), OnLongClickListener {

                init {
                    itemView.setOnLongClickListener(this)
                }

                override fun onLongClick(view: View): Boolean {
                        // Handle long click
                        // Return true to indicate the click was handled


                        contactList.removeAt(adapterPosition)
                        notifyItemRemoved(adapterPosition)
                        return true
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
                return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_all_layout, parent, false))
        }

        override fun getItemCount(): Int {
                return contactList.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
                val currItem = contactList[position]
                val img = holder.itemView.findViewById<ImageView>(R.id.imgPhoto2)
                val txtName = holder.itemView.findViewById<TextView>(R.id.txtName2)
                val txtPhone = holder.itemView.findViewById<TextView>(R.id.txtPhone2)

                val context = holder.itemView.context
                val dir = context.filesDir.absolutePath
                val imgFile = File(dir + "//" + currItem.imagePath)
                if (imgFile.exists()) {
                        val myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
                        img.setImageBitmap(myBitmap)
                }

                txtName.text = currItem.name
                txtPhone.text = currItem.phone
        }

        fun setData(contacts : List<Contact>) {
                this.contactList = contacts.toMutableList()
                notifyDataSetChanged()
        }
}