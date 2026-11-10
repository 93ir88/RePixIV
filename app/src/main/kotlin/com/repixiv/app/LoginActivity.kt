package com.repixiv.app

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.repixiv.app.pixiv.Pixiv

/** pixiv OAuth (PKCE) login: the web login page redirects to pixiv://account/login?code=... */
class LoginActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var exchanging = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Pixiv.init(applicationContext)

        val root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val i = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(i.left, i.top, i.right, i.bottom)
            WindowInsetsCompat.CONSUMED
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (web.canGoBack()) web.goBack() else finish()
                }
            },
        )

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                handle(request.url)
        }

        if (savedInstanceState == null) {
            CookieManager.getInstance().removeAllCookies(null)
            web.loadUrl(Pixiv.newLoginUrl())
        } else {
            web.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    private fun handle(uri: Uri): Boolean {
        val scheme = uri.scheme ?: return false
        if (scheme == "http" || scheme == "https") return false
        if (scheme == "pixiv") {
            val code = uri.getQueryParameter("code")
            if (code != null && !exchanging) finishLogin(code)
        }
        return true
    }

    private fun finishLogin(code: String) {
        exchanging = true
        Toast.makeText(this, "Signing in…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                Pixiv.exchangeCodeBlocking(code)
                runOnUiThread { finish() }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
