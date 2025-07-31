package com.photoresizer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        with(webView.settings) {
            javaScriptEnabled = true
            allowFileAccess = true           // Required for local asset/image loading
            allowContentAccess = true        // Needed for content:// access
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
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true // Block all external links
        }

        webView.addJavascriptInterface(Saver(this), "AndroidSaver")

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

    class Saver(private val context: Context) {
        @JavascriptInterface
        fun saveBase64Image(base64Data: String, mimeType: String) {
            try {
                val base64 = base64Data.substringAfter("base64,", "")
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val ext = if (mimeType == "image/png") "png" else "jpg"
                val fileName = "resized_${System.currentTimeMillis()}.$ext"
                val file = File(context.getExternalFilesDir(null), fileName)
                FileOutputStream(file).use { it.write(bytes) }

                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
