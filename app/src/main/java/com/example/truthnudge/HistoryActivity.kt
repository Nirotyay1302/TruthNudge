package com.example.truthnudge // Ensure this matches your directory structure

// AndroidX and System Imports should come first
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Your project-specific imports
import com.example.truthnudge.data.AppDatabase
// Make sure ClaimItem is defined and imported correctly.
// If ClaimItem.kt is in the same 'com.example.truthnudge' package, this explicit import might not be strictly needed,
// but it's good for clarity or if it's in a sub-package like com.example.truthnudge.models.
import com.example.truthnudge.ClaimItem // OR com.example.truthnudge.models.ClaimItem

// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Java Util
import java.util.Date

// Commented out: Assuming your actual ClaimEntity (from Room) is defined elsewhere
// and db.claimDao().getAllClaims() returns a list of that entity.
// data class ClaimEntity(
//    @PrimaryKey val id: String, // Or Int
//    val text: String,
//    val verdict: String,
//    val timestamp: Long
// )

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var claimsAdapter: ClaimsAdapter // This requires ClaimsAdapter to be defined and accessible
    private lateinit var emptyHistoryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history) // Requires R and activity_history to be resolvable

        // Initialize views
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyHistoryText = findViewById(R.id.emptyHistoryText)

        // Initialize Adapter
        // This requires ClaimsAdapter to be defined and its constructor to match.
        // Also, ClaimItem needs to be resolvable.
        claimsAdapter = ClaimsAdapter(mutableListOf()) { clickedClaimItem ->
            // Handle item click if needed
            Log.d("HistoryActivity", "Clicked: ${clickedClaimItem.text} at ${Date(clickedClaimItem.timestamp)}")
            // You could open a detail view, etc.
        }

        // Setup RecyclerView
        historyRecyclerView.adapter = claimsAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        loadClaimsFromDatabase()
    }

    private fun loadClaimsFromDatabase() {
        // Ensure AppDatabase and its getDatabase method are correctly implemented
        val db = AppDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            // This assumes db.claimDao().getAllClaims() returns a List<YourActualClaimEntity>
            // and YourActualClaimEntity has 'text', 'verdict', and 'timestamp' properties.
            val claimEntities = db.claimDao().getAllClaims()

            // Map your Room entities to ClaimItem objects for the adapter
            // Inside HistoryActivity.kt, in the loadClaimsFromDatabase() function:

            val claimItemsForAdapter = claimEntities.map { entity ->
                ClaimItem(
                    id = entity.id, // Assuming 'entity' from your database has an 'id' field
                    text = "Claim: ${entity.text}\nVerdict: ${entity.verdict}",
                    timestamp = entity.timestamp
                )
            }

            withContext(Dispatchers.Main) {
                if (claimItemsForAdapter.isEmpty()) {
                    historyRecyclerView.visibility = View.GONE
                    emptyHistoryText.visibility = View.VISIBLE
                } else {
                    historyRecyclerView.visibility = View.VISIBLE
                    emptyHistoryText.visibility = View.GONE
                    // Ensure ClaimsAdapter has an 'updateClaims' method that accepts List<ClaimItem>
                    claimsAdapter.updateClaims(claimItemsForAdapter)
                }
            }
        }
    }
}
