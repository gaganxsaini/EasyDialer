package com.example.easydialer.data

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.easydialer.R

/**
 * The dial screen: swipeable pages, each a grid of [perScreen] photos that fills the screen.
 * With 1 per screen it is the original one-big-photo-per-page layout.
 *
 * Every bind sets each tile's image, overlay and tap action from scratch, because RecyclerView
 * reuses pages: anything left over would show one person's face while calling someone else.
 */
class ListAdapter(
    private val onCall: (ContactCard) -> Unit,
    private val onLongPress: () -> Unit,
) : RecyclerView.Adapter<ListAdapter.PageHolder>() {
    private var cards = emptyList<ContactCard>()
    private var pages = emptyList<List<ContactCard>>()
    private var perScreen = 1

    /** One spot in the grid: the photo, plus a red frame and badge shown after a missed call. */
    class Tile(val frame: FrameLayout, val photo: ImageView, val missedBorder: View, val missedBadge: ImageView)

    class PageHolder(itemView: View, val tiles: List<Tile>) : RecyclerView.ViewHolder(itemView)

    /** [cards] in display order; people with a missed call should already be first. */
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
        val gap = if (viewType > 1) dp(context, 2) else 0
        val tiles = mutableListOf<Tile>()

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
                val tile = createTile(context, badgeSize(context, viewType))
                row.addView(tile.frame, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(gap, gap, gap, gap)
                })
                tiles += tile
            }
        }
        return PageHolder(page, tiles)
    }

    private fun createTile(context: Context, badgeSize: Int): Tile {
        val match = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val frame = FrameLayout(context)
        val photo = ImageView(context)
        val border = View(context).apply { setBackgroundResource(R.drawable.missed_call_border) }
        val badge = ImageView(context).apply {
            setBackgroundResource(R.drawable.missed_call_badge)
            setImageResource(R.drawable.ic_missed_call)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val pad = badgeSize / 5
            setPadding(pad, pad, pad, pad)
        }
        frame.addView(photo, match)
        frame.addView(border, match)
        val margin = dp(context, 14)
        frame.addView(badge, FrameLayout.LayoutParams(badgeSize, badgeSize, Gravity.TOP or Gravity.END).apply {
            setMargins(margin, margin, margin, margin)
        })
        return Tile(frame, photo, border, badge)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val page = pages[position]
        holder.tiles.forEachIndexed { index, tile ->
            val card = page.getOrNull(index)
            if (card == null) clearTile(tile) else bindTile(tile, card)
        }
    }

    private fun bindTile(tile: Tile, card: ContactCard) {
        val missed = card.missedCallAt != null
        tile.frame.visibility = View.VISIBLE
        tile.missedBorder.isVisible = missed
        tile.missedBadge.isVisible = missed
        tile.frame.contentDescription = if (missed) {
            tile.frame.context.getString(R.string.missed_call_from, card.name ?: card.number ?: "")
        } else {
            card.name
        }
        tile.frame.setOnClickListener { onCall(card) }
        tile.frame.setOnLongClickListener {
            onLongPress()
            true
        }

        val img = tile.photo
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
    private fun clearTile(tile: Tile) {
        Glide.with(tile.photo).clear(tile.photo)
        tile.photo.setImageDrawable(null)
        tile.frame.setOnClickListener(null)
        tile.frame.setOnLongClickListener(null)
        tile.frame.isClickable = false
        tile.frame.isLongClickable = false
        tile.frame.contentDescription = null
        tile.frame.visibility = View.INVISIBLE
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

        /** The missed-call badge shrinks as tiles get smaller. */
        fun badgeSize(context: Context, perScreen: Int): Int = dp(
            context,
            when (perScreen) {
                1 -> 120
                2 -> 96
                4, 6 -> 72
                else -> 52
            }
        )

        fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    }
}
