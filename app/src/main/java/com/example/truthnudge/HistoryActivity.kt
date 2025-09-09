package com.example.truthnudge

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.truthnudge.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        container = findViewById(R.id.historyContainer)

        val db = AppDatabase.getDatabase(this)

        // Load all claims
        CoroutineScope(Dispatchers.IO).launch {
            val claims = db.claimDao().getAllClaims()
            runOnUiThread {
                claims.forEach { claim ->
                    val textView = TextView(this@HistoryActivity).apply {
                        text = "Claim: ${claim.text}\nVerdict: ${claim.verdict}"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    container.addView(textView)
                }
            }
        }
    }
}
