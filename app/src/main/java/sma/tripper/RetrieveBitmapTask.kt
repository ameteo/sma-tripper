package sma.tripper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.net.URL

class RetrieveBitmapTask(val bitmapConsumer: (Bitmap) -> Unit): AsyncTask<String, Void, Bitmap>() {
    override fun doInBackground(vararg sources: String?): Bitmap {
        val source = URL(sources[0])
        return BitmapFactory.decodeStream(source.openConnection().getInputStream())
    }

    override fun onPostExecute(bitmap: Bitmap?) {
        bitmapConsumer(bitmap!!)
    }
}