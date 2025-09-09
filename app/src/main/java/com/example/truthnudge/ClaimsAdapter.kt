package com.example.truthnudge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ClaimsAdapter(
    var claims: MutableList<String>,
    private val onClaimClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ClaimsAdapter.ClaimViewHolder>() {
    val claimList: List<String>
        get() = claims
    inner class ClaimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val claimText: TextView = itemView.findViewById(R.id.claimText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaimViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_claim, parent, false)
        return ClaimViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClaimViewHolder, position: Int) {
        val claim = claims[position]
        holder.claimText.text = claim

        // Fade-in animation
        holder.itemView.alpha = 0f
        holder.itemView.animate().alpha(1f).setDuration(300).start()

        // Click listener
        holder.itemView.setOnClickListener {
            onClaimClick?.invoke(claims[position])
        }
    }

    override fun getItemCount(): Int = claims.size

    fun updateClaims(newClaims: List<String>) {
        claims = newClaims.toMutableList()
        notifyDataSetChanged()
    }

    fun addClaims(claim: String) {
        claims.add(0, claim)
        notifyDataSetChanged()
    }
    fun removeClaim(position: Int) {
        if (position in claims.indices) {
            claims.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
