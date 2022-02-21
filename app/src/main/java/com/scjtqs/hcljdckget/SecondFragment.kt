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
        //下面这些直接复制就好
        webView?.webViewClient = webClient

        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true  // 开启 JavaScript 交互
        webSettings.setAppCacheEnabled(true) // 启用或禁用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT // 只要缓存可用就加载缓存, 哪怕已经过期失效 如果缓存不可用就从网络上加载数据
//        webSettings.setAppCachePath(cacheDir.path) // 设置应用缓存路径

        // 缩放操作
        webSettings.setSupportZoom(false) // 支持缩放 默认为true 是下面那个的前提
        webSettings.builtInZoomControls = false // 设置内置的缩放控件 若为false 则该WebView不可缩放
        webSettings.displayZoomControls = false // 隐藏原生的缩放控件

        webSettings.blockNetworkImage = false // 禁止或允许WebView从网络上加载图片
        webSettings.loadsImagesAutomatically = true // 支持自动加载图片

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.safeBrowsingEnabled = true // 是否开启安全模式
        }

        webSettings.javaScriptCanOpenWindowsAutomatically = true // 支持通过JS打开新窗口
        webSettings.domStorageEnabled = true // 启用或禁用DOM缓存
        webSettings.setSupportMultipleWindows(true) // 设置WebView是否支持多窗口

        // 设置自适应屏幕, 两者合用
        webSettings.useWideViewPort = true  // 将图片调整到适合webview的大小
        webSettings.loadWithOverviewMode = true  // 缩放至屏幕的大小
        webSettings.allowFileAccess = true // 设置可以访问文件

        webSettings.setGeolocationEnabled(true) // 是否使用地理位置
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
        // 先切割 ;
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
            // todo 推送到hcl地址
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
            //异步请求
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    L.d("UPDATE onFailure: $e")
                    dialog.setTitleMessage("推送失败", e.toString())
                    fragmentManager?.let { dialog.show(it, "MessageDialog") }

                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val rsp = response.body!!.string()
                    L.d("UPDATE OnResponse: " + rsp)
                    dialog.setTitleMessage("推送成功", rsp)
                    fragmentManager?.let { dialog.show(it, "MessageDialog") }
                }
            })
        } catch (e: Exception) {
            L.e("UPDATE ERROR: $e")
        }
    }
}