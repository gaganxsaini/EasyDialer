package com.example.easydialer.data

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.easydialer.R

/**
 * The dial screen: swipeable pages, each a grid of [perScreen] photos that fills the screen.
 * With 1 per screen it is the original one-big-photo-per-page layout.
 *
 * Every bind sets each tile's image, background and tap action from scratch, because RecyclerView
 * reuses pages: anything left over would show one person's face while calling someone else.
 */
class ListAdapter(
    private val onCall: (ContactCard) -> Unit,
    private val onLongPress: () -> Unit,
) : RecyclerView.Adapter<ListAdapter.PageHolder>() {
    private var cards = emptyList<ContactCard>()
    private var pages = emptyList<List<ContactCard>>()
    private var perScreen = 1

    class PageHolder(itemView: View, val tiles: List<ImageView>) : RecyclerView.ViewHolder(itemView)

    fun setData(cards: List<ContactCard>) {
        this.cards = cards
        rebuildPages()
    }

    /** Returns true if the grid size changed. */
    fun setPerScreen(count: Int): Boolean {
        if (count == perScreen) return false
        perScreen = count
        rebuildPages()
        return true
    }

    private fun rebuildPages() {
        pages = cards.chunked(perScreen)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = pages.size

    // Pages built for one grid size can't be reused for another.
    override fun getItemViewType(position: Int): Int = perScreen

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val (columns, rows) = gridShape(viewType)
        val context = parent.context
        val gap = (2 * context.resources.displayMetrics.density).toInt()
        val tiles = mutableListOf<ImageView>()

        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }
        repeat(rows) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            page.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            repeat(columns) {
                val tile = ImageView(context)
                // A thin black gap between tiles, only when there is more than one.
                val margin = if (viewType > 1) gap else 0
                row.addView(tile, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(margin, margin, margin, margin)
                })
                tiles += tile
            }
        }
        return PageHolder(page, tiles)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = pages[position]
        holder.tiles.forEachIndexed { index, tile ->
            val card = page.getOrNull(index)
            if (card == null) clearTile(tile) else bindTile(tile, card)
        }
    }

    private fun bindTile(img: ImageView, card: ContactCard) {
        img.visibility = View.VISIBLE
        img.contentDescription = card.name
        img.setOnClickListener { onCall(card) }
        img.setOnLongClickListener {
            onLongPress()
            true
        }

        if (card.photoUri != null) {
            img.scaleType = ImageView.ScaleType.CENTER_CROP
            img.setBackgroundColor(Color.BLACK)
            // Glide shrinks the photo to the tile size; RGB_565 halves the memory again.
            Glide.with(img)
                .load(card.photoUri)
                .format(DecodeFormat.PREFER_RGB_565)
                .error(R.drawable.ic_person)
                .into(img)
        } else {
            // No photo: a person icon on a colour that stays the same for each contact.
            Glide.with(img).clear(img)
            img.scaleType = ImageView.ScaleType.CENTER_INSIDE
            img.setBackgroundColor(ContactColors.forContact(card.contact))
            img.setImageResource(R.drawable.ic_person)
        }
    }

    /** Empty spot on the last page: shows nothing and does nothing when tapped. */
    private fun clearTile(img: ImageView) {
        Glide.with(img).clear(img)
        img.setImageDrawable(null)
        img.setOnClickListener(null)
        img.setOnLongClickListener(null)
        img.isClickable = false
        img.isLongClickable = false
        img.contentDescription = null
        img.visibility = View.INVISIBLE
    }

    private companion object {
        /** Columns and rows for each choice; the phone is tall, so extra rows before extra columns. */
        fun gridShape(perScreen: Int): Pair<Int, Int> = when (perScreen) {
            2 -> 1 to 2
            4 -> 2 to 2
            6 -> 2 to 3
            9 -> 3 to 3
            else -> 1 to 1
        }
    }
}
