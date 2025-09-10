package com.example.truthnudge

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.truthnudge.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyScrollView: ScrollView // Changed from historyRecycler for clarity
    private lateinit var historyContainer: LinearLayout // Renamed from 'container' for clarity
    private lateinit var emptyHistoryText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize views
        historyScrollView = findViewById(R.id.historyRecycler) // Make sure this ID matches your ScrollView
        historyContainer = findViewById(R.id.historyContainer)
        emptyHistoryText = findViewById(R.id.emptyHistoryText) // The new TextView for "No history"

        val db = AppDatabase.getDatabase(this)

        // Load all claims
        CoroutineScope(Dispatchers.IO).launch {
            val claims = db.claimDao().getAllClaims()

            // Switch to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                if (claims.isEmpty()) {
                    // No history, show the "No history" message
                    historyScrollView.visibility = View.GONE
                    emptyHistoryText.visibility = View.VISIBLE
                } else {
                    // History exists, show the ScrollView and populate it
                    historyScrollView.visibility = View.VISIBLE
                    emptyHistoryText.visibility = View.GONE

                    historyContainer.removeAllViews() // Clear previous views if any
                    claims.forEach { claim ->
                        val textView = TextView(this@HistoryActivity).apply {
                            text = "Claim: ${claim.text}\nVerdict: ${claim.verdict}"
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                        }
                        historyContainer.addView(textView)
                    }
                }
            }
        }
    }
}
