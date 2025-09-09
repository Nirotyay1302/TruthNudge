package com.example.truthnudge

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.truthnudge.BuildConfig.GEMINI_API_KEY
import com.example.truthnudge.data.AppDatabase
import com.example.truthnudge.data.Claim
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var verdictView: TextView
    private lateinit var nudgeMessage: TextView
    private lateinit var receivedText: TextView
    private lateinit var sourceLinks: TextView
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receiver)

        receivedText = findViewById(R.id.receivedText)
        verdictView = findViewById(R.id.verdictText)
        nudgeMessage = findViewById(R.id.nudgeMessage)
        sourceLinks = findViewById(R.id.sourceLinks)
        val cancelButton: MaterialButton = findViewById(R.id.cancelButton)
        val shareAnywayButton: MaterialButton = findViewById(R.id.shareAnywayButton)

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: "No text received"
        receivedText.text = sharedText

        // Check if intent contains a link
        val urlRegex = "(http|https)://[\\w\\-._~:/?#@!$&'()*+,;=%]+".toRegex()

        val urls = urlRegex.findAll(sharedText).map { it.value }.toList()

        if (urls.isNotEmpty()) {
            checkLinkWithGemini(urls.first())
        }

        cancelButton.setOnClickListener { finish() }

        shareAnywayButton.setOnClickListener {
            val bitmap = createShareableCard(
                receivedText.text.toString(),
                verdictView.text.toString(),
                nudgeMessage.text.toString(),
                sourceLinks.text.toString()
            )
            shareBitmap(bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun checkLinkWithGemini(url: String) {
        val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = GEMINI_API_KEY)
        val db = AppDatabase.getDatabase(this)

        activityScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze the following URL and tell if it is safe to open.
                    Respond ONLY in JSON format:
                    {
                        "verdict": "🟢 Safe / 🔴 Unsafe / ⚠️ Suspicious",
                        "reason": "short explanation why",
                        "source": "domain or N/A"
                    }
                    URL: "$url"
                """.trimIndent()

                val responseText = withTimeout(15000) {
                    model.generateContent(prompt).text ?: "{}"
                }

                val cleanResponse = responseText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val jsonResult = JSONObject(cleanResponse)
                val verdict = jsonResult.optString("verdict", "⚪ Unknown")
                val reason = jsonResult.optString("reason", "")
                val sources = jsonResult.optString("source", "N/A")

                // Save link claim in database
                db.claimDao().insertClaim(
                    Claim(
                        text = url,
                        verdict = verdict,
                        reason = reason,
                        source = sources,
                        timestamp = System.currentTimeMillis()
                    )
                )

                withContext(Dispatchers.Main) {
                    showSafetyDialog(url, verdict, reason, sources)
                }
            } catch (e: Exception) {
                Log.e("AI_LINK_ERROR", "Failed to analyze URL", e)
                withContext(Dispatchers.Main) {
                    showSafetyDialog(url, "⚪ Error", "Could not check with AI", "N/A")
                }
            }catch (e: QuotaExceededException) {
                Log.e("AI_ERROR", "Quota exceeded!", e)

                val verdict = "⚪ AI unavailable"
                val reason = "Quota exceeded – try again later"
                val source = "N/A"

                db.claimDao().insertClaim(
                    Claim(
                        text = url,
                        verdict = verdict,
                        reason = reason,
                        source = source,
                        timestamp = System.currentTimeMillis()
                    )
                )

                withContext(Dispatchers.Main) {
                    verdictView.text = verdict
                    nudgeMessage.text = reason
                    sourceLinks.text = source
                }
            }
        }
    }


    private fun showSafetyDialog(url: String, verdict: String, reason: String, source: String) {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("Link Safety Check (AI)")
            .setMessage("Verdict: $verdict\nReason: $reason\nSource: $source\n\nURL: $url")
            .setPositiveButton("Open Anyway") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton("Cancel", null)
            .show()

        // Update UI safely
        verdictView.text = verdict
        nudgeMessage.text = reason
        sourceLinks.text = source
    }

    private fun shareBitmap(bitmap: Bitmap) {
        try {
            val file = File(cacheDir, "claim_verification.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Verification"))
        } catch (e: Exception) {
            Log.e("SHARE_ERROR", "Failed to share bitmap", e)
        }
    }

    private fun createShareableCard(
        claim: String,
        verdict: String,
        reason: String,
        source: String
    ): Bitmap {
        val cardView = layoutInflater.inflate(R.layout.share_card_layout, null) as MaterialCardView
        val claimText = cardView.findViewById<TextView>(R.id.shareClaimText)
        val verdictText = cardView.findViewById<TextView>(R.id.shareVerdictText)
        val reasonText = cardView.findViewById<TextView>(R.id.shareReasonText)
        val sourceText = cardView.findViewById<TextView>(R.id.shareSourceText)

        claimText.text = claim
        verdictText.text = verdict
        reasonText.text = reason
        sourceText.text = "Source: $source"

        val color = when {
            verdict.contains("True") || verdict.contains("Safe") -> android.R.color.holo_green_dark
            verdict.contains("False") || verdict.contains("Unsafe") -> android.R.color.holo_red_dark
            else -> android.R.color.holo_orange_dark
        }
        verdictText.setTextColor(ContextCompat.getColor(this, color))

        val maxWidth = resources.displayMetrics.widthPixels - 32
        cardView.measure(
            View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        cardView.layout(0, 0, cardView.measuredWidth, cardView.measuredHeight)

        return Bitmap.createBitmap(
            cardView.width.takeIf { it > 0 } ?: 500,
            cardView.height.takeIf { it > 0 } ?: 300,
            Bitmap.Config.ARGB_8888
        ).also { cardView.draw(Canvas(it)) }
    }
}
