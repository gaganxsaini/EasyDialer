package com.example.easydialer.data

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.easydialer.R

/**
 * Full-screen photo pages. Every bind sets the image, background and tap action from scratch,
 * because RecyclerView reuses pages: anything left over would show one person's face while
 * calling someone else.
 */
class ListAdapter(
    private val onCall: (ContactCard) -> Unit,
    private val onLongPress: () -> Unit,
) : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {
    private var cards = emptyList<ContactCard>()

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.contact_layout, parent, false))
    }

    override fun getItemCount(): Int = cards.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val card = cards[position]
        val img = holder.img

        img.contentDescription = card.name
        img.setOnClickListener { onCall(card) }
        img.setOnLongClickListener {
            onLongPress()
            true
        }

        if (card.photoUri != null) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setBackgroundColor(Color.BLACK)
            // Glide shrinks the photo to the screen size; RGB_565 halves the memory again.
            Glide.with(img)
                .load(card.photoUri)
                .format(DecodeFormat.PREFER_RGB_565)
                .error(R.drawable.ic_person)
                .into(img)
        } else {
            showPlaceholder(img, card)
        }
    }

    /** No photo: a person icon on a colour that stays the same for each contact, so pages can be told apart. */
    private fun showPlaceholder(img: ImageView, card: ContactCard) {
        Glide.with(img).clear(img)
        img.scaleType = ImageView.ScaleType.CENTER_INSIDE
        img.setBackgroundColor(ContactColors.forContact(card.contact))
        img.setImageResource(R.drawable.ic_person)
    }

    fun setData(cards: List<ContactCard>) {
        this.cards = cards
        notifyDataSetChanged()
    }
}
