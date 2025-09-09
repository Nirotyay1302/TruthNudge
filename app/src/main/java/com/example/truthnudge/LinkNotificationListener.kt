package com.example.truthnudge

import android.R.attr.text
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.example.truthnudge.data.AppDatabase
import com.example.truthnudge.data.Claim
import com.google.ai.client.generativeai.type.QuotaExceededException
import kotlinx.coroutines.*
import org.json.JSONObject

class LinkNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        // Extract only URLs
        val urlRegex = "(http|https)://[\\w\\-._~:/?#@!$&'()*+,;=%]+".toRegex()

        val urls = urlRegex.findAll(text).map { it.value }.toList()

        if (urls.isEmpty()) {
            Log.d("NotifListener", "No URL found in notification → ignored")
            return
        }

        urls.forEach { url ->
            Log.d("NotifListener", "Captured URL: $url")
            checkWithGemini(url)
        }
    }

    private fun checkWithGemini(url: String) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
        val db = AppDatabase.getDatabase(this)

        serviceScope.launch {
            try {
                val prompt = """
                    Analyze the following URL and return in JSON:
                    {
                        "verdict": "🟢 Safe / 🔴 Unsafe / ⚠️ Suspicious / 🟡 Uncertain",
                        "reason": "short explanation",
                        "source": "URL or N/A"
                    }
                    URL: "$url"
                """.trimIndent()

                val responseText = model.generateContent(prompt).text ?: "{}"
                val cleanResponse = responseText
                    .trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

                val json = JSONObject(cleanResponse)
                val verdict = json.optString("verdict", "⚪ Unknown")
                val reason = json.optString("reason", "N/A")
                val source = json.optString("source", "N/A")

                // Save to DB
                db.claimDao().insertClaim(
                    Claim(
                        text = url,
                        verdict = verdict,
                        reason = reason,
                        source = source,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // 🔔 Show local alert if unsafe/suspicious
                if (verdict.contains("Unsafe") || verdict.contains("Suspicious") || verdict.contains("False")) {
                    showAlertNotification(verdict, reason, url)
                }

            } catch (e: QuotaExceededException) {
                Log.e("GeminiCheck", "Quota exceeded!", e)

                val verdict = "⚪ AI unavailable"
                val reason = "Quota exceeded – try again later"
                val source = "N/A"

                // Save fallback claim
                db.claimDao().insertClaim(
                    Claim(
                        text = url,
                        verdict = verdict,
                        reason = reason,
                        source = source,
                        timestamp = System.currentTimeMillis()
                    )
                )
                showAlertNotification(verdict, reason, url)

            } catch (e: Exception) {
                Log.e("GeminiCheck", "Error checking text", e)
            }
        }
    }

    private fun showAlertNotification(verdict: String, reason: String, url: String) {
        val channelId = "truthnudge_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TruthNudge Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
            val intent = Intent(this, ShareReceiverActivity::class.java).apply {
                putExtra("claim_text", url)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                url.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("⚠️ Content Alert: $verdict")
                .setContentText(reason.takeIf { it.isNotBlank() } ?: "Tap for details")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText("URL: $url\n\nReason: $reason")
                )
                .setSmallIcon(R.drawable.ic_warning) // must exist
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
