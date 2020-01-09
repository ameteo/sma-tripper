package sma.tripper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.lang.Integer.min
import java.net.URL

class RetrieveBitmapTask(val bitmapConsumer: (Bitmap) -> Unit): AsyncTask<String, Void, Bitmap?>() {
    override fun doInBackground(vararg sources: String?): Bitmap? {
        val source = URL(sources[0])
        try {
            return BitmapFactory.decodeStream(source.openConnection().getInputStream())
        } catch (e: Exception) {
            return null
        }
    }

    override fun onPostExecute(bitmap: Bitmap?) {
        if (bitmap != null) {
            val length = min(bitmap.width, bitmap.height)
            val croppedBitmap = Bitmap.createBitmap(bitmap, (bitmap.width - length) / 2, (bitmap.height - length) / 2, length, length)
            bitmapConsumer(croppedBitmap)
        }
    }
}