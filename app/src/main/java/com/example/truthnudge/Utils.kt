package com.example.truthnudge

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Utils {
    fun getImageUriFromBitmap(context: Context, bitmap: Bitmap): Uri {
        val file = File(context.cacheDir, "claim_verification.png")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
        return Uri.fromFile(file)
    }
}
