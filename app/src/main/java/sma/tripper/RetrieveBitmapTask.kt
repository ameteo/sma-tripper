package sma.tripper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.lang.Integer.min
import java.net.URL

class RetrieveBitmapTask(val bitmapConsumer: (Bitmap) -> Unit): AsyncTask<String, Void, Bitmap>() {
    override fun doInBackground(vararg sources: String?): Bitmap {
        val source = URL(sources[0])
        return BitmapFactory.decodeStream(source.openConnection().getInputStream())
    }

    override fun onPostExecute(bitmap: Bitmap?) {
        val originalBitmap = bitmap!!
        val length = min(originalBitmap.width, originalBitmap.height)
        val croppedBitmap = Bitmap.createBitmap(originalBitmap, (originalBitmap.width - length) / 2, (originalBitmap.height - length) / 2, length, length)
        bitmapConsumer(croppedBitmap)
    }
}