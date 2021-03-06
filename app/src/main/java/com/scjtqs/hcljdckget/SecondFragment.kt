package com.scjtqs.hcljdckget


import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*

import android.util.Log
import androidx.fragment.app.DialogFragment
import com.safframework.log.L
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.net.URLEncoder


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : DialogFragment() {

    private var webView: WebView? = null
    var WEB_URL = "https://m.jd.com"
    private suspend fun getResult(num: Int): Int {
        delay(5000)
        return num * num
    }

    private var pt_pin = ""
    private var pt_key = ""

    val scope = MainScope()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.webview, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webview)
        webView?.loadUrl(WEB_URL)

        val webClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
//                super.onPageFinished(view, url)
                val cookies = CookieManager.getInstance().getCookie(url);
//                L.d("cookies= $cookies")
                if (cookies.isNullOrEmpty()) {
                    return
                }
                scope.launch {
                    val check = async {
                        parseCookie(cookies)
                    }
                }
            }
        }
        //??????????????????????????????
        webView?.webViewClient = webClient

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true  // ?????? JavaScript ??????
        webSettings.setAppCacheEnabled(true) // ?????????????????????
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // ?????????????????????????????????, ???????????????????????? ????????????????????????????????????????????????
//        webSettings.setAppCachePath(cacheDir.path) // ????????????????????????

        // ????????????
        webSettings.setSupportZoom(false) // ???????????? ?????????true ????????????????????????
        webSettings.builtInZoomControls = false // ??????????????????????????? ??????false ??????WebView????????????
        webSettings.displayZoomControls = false // ???????????????????????????

        webSettings.blockNetworkImage = false // ???????????????WebView????????????????????????
        webSettings.loadsImagesAutomatically = true // ????????????????????????

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.safeBrowsingEnabled = true // ????????????????????????
        }

        webSettings.javaScriptCanOpenWindowsAutomatically = true // ????????????JS???????????????
        webSettings.domStorageEnabled = true // ???????????????DOM??????
        webSettings.setSupportMultipleWindows(true) // ??????WebView?????????????????????

        // ?????????????????????, ????????????
        webSettings.useWideViewPort = true  // ????????????????????????webview?????????
        webSettings.loadWithOverviewMode = true  // ????????????????????????
        webSettings.allowFileAccess = true // ????????????????????????

        webSettings.setGeolocationEnabled(true) // ????????????????????????
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null);
        cookieManager.setAcceptCookie(true);
        webView?.fitsSystemWindows = true
        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView?.loadUrl(WEB_URL)
        cookieManager.acceptThirdPartyCookies(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }


    suspend fun parseCookie(cookies: String) {
        // retina=1; cid=9; webp=1; visitkey=1004687458115152;
        // ????????? ;
        if (this.pt_pin.isNotEmpty() && this.pt_key.isNotEmpty()) {
            return
        }
        val list = cookies.split(";")
        list.forEach { cookieStr ->
            val cookie = cookieStr.split("=")
            when (cookie[0].trim()) {
                "pt_pin" -> this.pt_pin = cookie[1].trim()
                "pt_key" -> this.pt_key = cookie[1].trim()
            }
//            L.d("cookie=${cookieStr}")
        }
        L.d("pt_pin=${this.pt_pin}; pt_key=${this.pt_key}")
        if (this.pt_pin.isNotEmpty() && this.pt_key.isNotEmpty()) {
            // todo ?????????hcl??????
            pushCookie()
        }
    }

    fun pushCookie() {
        val ck = "pt_pin=${this.pt_pin};pt_key=${this.pt_key};"
        val baseUrl = "https://jd.900109.xyz:8443/notify"
        val url = baseUrl + "?hhkb=" + URLEncoder.encode(ck, "utf-8")
        val dialog = MessageDialog()
        L.d("start pushCookie url=${url}, pt_pin=${this.pt_pin} , pt_key=${this.pt_key}")
        try {
            val client = OkHttpClient()
            val request = Request.Builder().get()
                .url(url)
                .build()
            val response = client.newCall(request)
            val call = client.newCall(request)
            //????????????
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    L.d("UPDATE onFailure: $e")
                    dialog.setTitleMessage("????????????", e.toString())
                    fragmentManager?.let { dialog.show(it, "MessageDialog") }

                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val rsp = response.body!!.string()
                    L.d("UPDATE OnResponse: " + rsp)
                    dialog.setTitleMessage("????????????", rsp)
                    fragmentManager?.let { dialog.show(it, "MessageDialog") }
                }
            })
        } catch (e: Exception) {
            L.e("UPDATE ERROR: $e")
        }
    }
}