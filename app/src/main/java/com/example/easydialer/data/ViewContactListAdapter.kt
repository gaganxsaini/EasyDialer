package com.example.easydialer.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydialer.R
import java.io.File


class ViewContactListAdapter : RecyclerView.Adapter<ViewContactListAdapter.MyViewHolder>() {
        inner class MyViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        }

        private val differCallback = object : DiffUtil.ItemCallback<Contact>(){
                override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                        return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
                        return oldItem == newItem
                }
        }

        val differ = AsyncListDiffer(this, differCallback)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
                return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_all_layout, parent, false))
        }

        override fun getItemCount(): Int = differ.currentList.size

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

                with(holder) {
                        with (differ.currentList[position]) {
                                val img = holder.itemView.findViewById<ImageView>(R.id.imgPhoto2)
                                val txtName = holder.itemView.findViewById<TextView>(R.id.txtName2)
                                val txtPhone = holder.itemView.findViewById<TextView>(R.id.txtPhone2)

                                val context = holder.itemView.context
                                val dir = context.filesDir.absolutePath
                                val imgFile = File(dir + "//" + imagePath)
                                if (imgFile.exists()) {
                                        Glide.with(holder.itemView.context).load(imgFile).into(img)
                                }

                                txtName.text = name
                                txtPhone.text = phone
                        }
                }

        }
}