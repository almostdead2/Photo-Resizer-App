package com.photoresizer

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        with(webView.settings) {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            domStorageEnabled = true
            setSupportZoom(false)
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    val intent = fileChooserParams.createIntent()
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    false
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
        }

        // Attach JavaScript interface
        webView.addJavascriptInterface(Saver(this, webView), "AndroidSaver")

        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val result: Array<Uri>? = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            fileChooserCallback?.onReceiveValue(result)
            fileChooserCallback = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    class Saver(private val context: Context, private val webView: WebView) {
        @JavascriptInterface
        fun saveBase64Image(base64Data: String, mimeType: String) {
            try {
                val base64 = base64Data.substringAfter("base64,", "")
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val ext = if (mimeType == "image/png") "png" else "jpg"
                val fileName = "resized_${System.currentTimeMillis()}.$ext"

                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoResizer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { out: OutputStream? ->
                        out?.write(bytes)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    (context as? Activity)?.runOnUiThread {
                        Toast.makeText(context, "Saved to Pictures/PhotoResizer", Toast.LENGTH_LONG).show()
                        webView.evaluateJavascript("window.onSaveComplete(true);", null) // ✅ Notifies JS success
                    }
                } else {
                    throw Exception("Failed to create MediaStore entry.")
                }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                    webView.evaluateJavascript("window.onSaveComplete(false);", null) // ✅ Notifies JS failure
                }
            }
        }
    }
}
