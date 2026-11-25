package com.example.easydialer.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.easydialer.R

/** The list on the setup screen. Names are already resolved, so binding does no contact lookups. */
class ViewContactListAdapter : RecyclerView.Adapter<ViewContactListAdapter.MyViewHolder>() {
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName2)
    }

    private val differCallback = object : DiffUtil.ItemCallback<ContactCard>() {
        override fun areItemsTheSame(oldItem: ContactCard, newItem: ContactCard): Boolean =
            oldItem.contact.id == newItem.contact.id

        override fun areContentsTheSame(oldItem: ContactCard, newItem: ContactCard): Boolean =
            oldItem == newItem
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_all_layout, parent, false))
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val card = differ.currentList[position]
        holder.txtName.text = card.name ?: card.number ?: holder.itemView.context.getString(R.string.unknown_contact)
    }
}
