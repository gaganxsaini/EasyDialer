package com.example.easydialer.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.easydialer.R

/**
 * The list on the setup screen. Rows can be dragged into a new order; while a drag is going on,
 * new lists from the database are held back so the row doesn't jump under her finger.
 */
class ViewContactListAdapter(
    private val onClick: (ContactCard) -> Unit,
) : RecyclerView.Adapter<ViewContactListAdapter.MyViewHolder>() {
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName2)
        val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
    }

    private val items = mutableListOf<ContactCard>()
    private var pending: List<ContactCard>? = null

    var dragging = false
        set(value) {
            field = value
            if (!value) pending?.let { submit(it) }
        }

    /** Contacts in their current on-screen order. */
    val currentList: List<ContactCard> get() = items

    fun submit(cards: List<ContactCard>) {
        if (dragging) {
            pending = cards
            return
        }
        pending = null
        items.clear()
        items.addAll(cards)
        notifyDataSetChanged()
    }

    /** Moves a row while it is being dragged. */
    fun move(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_all_layout, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val card = items[position]
        holder.txtName.text = card.name ?: card.number ?: holder.itemView.context.getString(R.string.unknown_contact)
        holder.itemView.setOnClickListener { onClick(card) }

        val thumb = holder.imgThumb
        if (card.photoUri != null) {
            thumb.setBackgroundColor(0)
            Glide.with(thumb).load(card.photoUri).centerCrop().error(R.drawable.ic_person).into(thumb)
        } else {
            Glide.with(thumb).clear(thumb)
            thumb.setBackgroundColor(ContactColors.forContact(card.contact))
            thumb.setImageResource(R.drawable.ic_person)
        }
    }
}
