package com.example.truthnudge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
// import androidx.compose.ui.semantics.text // This import seems unused, check if needed
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClaimsAdapter(
    var claims: MutableList<ClaimItem>, // Correct: MutableList<ClaimItem>
    private val onClaimClick: ((ClaimItem) -> Unit)? = null // Correct: Passes ClaimItem
) : RecyclerView.Adapter<ClaimsAdapter.ClaimViewHolder>() {

    // Expose a read-only list of ClaimItem
    val claimList: List<ClaimItem>
        get() = claims.toList()

    inner class ClaimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val claimText: TextView = itemView.findViewById(R.id.claimText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText) // Make sure this ID exists in item_claim.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaimViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_claim, parent, false)
        return ClaimViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClaimViewHolder, position: Int) {
        val currentClaimItem = claims[position]
        holder.claimText.text = currentClaimItem.text // 'text' in ClaimItem is "Claim...\nVerdict..."
        holder.timestampText.text = formatTimestamp(currentClaimItem.timestamp)

        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()

        holder.itemView.setOnClickListener {
            onClaimClick?.invoke(currentClaimItem)
        }
    }

    override fun getItemCount(): Int = claims.size

    fun updateClaims(newClaims: List<ClaimItem>) {
        claims.clear()
        claims.addAll(newClaims)
        notifyDataSetChanged() // Consider DiffUtil for better performance on large/frequent updates
    }

    /**
     * Removes a claim from the specified position.
     * This method is now named as expected by HomeActivity.
     */
    fun removeClaimAt(position: Int) { // RENAMED/CONFIRMED (was removeClaim)
        if (position >= 0 && position < claims.size) {
            claims.removeAt(position)
            notifyItemRemoved(position)
            // If positions of other items change and need re-binding after removal:
            // notifyItemRangeChanged(position, claims.size - position)
        }
    }

    /**
     * Adds a claim at the specified position.
     * This method is now named and functions as expected by HomeActivity's undo logic.
     */
    fun addClaimAt(position: Int, claimItem: ClaimItem) { // NEW/MODIFIED (was addClaim, but specific to position)
        if (position >= 0 && position <= claims.size) { // Allow inserting at the end (position == claims.size)
            claims.add(position, claimItem)
            notifyItemInserted(position)
        } else if (position < 0) { // Fallback for invalid negative position: add to beginning
            claims.add(0, claimItem)
            notifyItemInserted(0)
        } else { // Fallback for invalid positive position beyond end: add to end
            claims.add(claimItem)
            notifyItemInserted(claims.size - 1)
        }
    }

    // You can keep this if you use it elsewhere for adding to the top,
    // or remove it if addClaimAt covers all your needs.
    /**
     * Adds a single new claim to the beginning of the list.
     */
    fun addClaimToTop(claimItem: ClaimItem) { // Renamed from addClaim to be more specific
        claims.add(0, claimItem)
        notifyItemInserted(0)
    }


    private fun formatTimestamp(timestampMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }
}
