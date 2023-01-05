package com.qiniudemo.baseapp.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import com.qiniu.baseapp.R
import com.qiniudemo.baseapp.BaseFragment
import com.qiniudemo.baseapp.manager.SchemaParser
import kotlinx.android.synthetic.main.fragment_web.*

class WebFragment : BaseFragment() {

    var webViewTitleCall: (title: String) -> Unit = {}

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private val webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) { //页面加载完成
            progressBar.visibility = View.GONE
        }

        override fun onPageStarted(
            view: WebView?,
            url: String?,
            favicon: Bitmap?
        ) { //页面开始加载
            progressBar.visibility = View.VISIBLE
        }
    }

    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private val webChromeClient: WebChromeClient = object : WebChromeClient() {
        //不支持js的alert弹窗，需要自己监听然后通过dialog弹窗
        override fun onJsAlert(
            webView: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            return false
        }

        //获取网页标题
        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            Log.i("web", "网页标题:$title")
            webViewTitleCall.invoke(title)
        }

        //加载进度回调
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progressBar.progress = newProgress
        }
    }

    private var startUrl  = ""
    @SuppressLint("SetJavaScriptEnabled")
    override fun initViewData() {
        webView.addJavascriptInterface(this, "router") //添加js监听 这样html就能调用客户端
        webView.webChromeClient = webChromeClient
        webView.webViewClient = webViewClient
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true //允许使用js
        //支持屏幕缩放
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.domStorageEnabled = true// 开启 DB storage API 功能settings.setDatabaseEnabled(true);// 开启 AppCacheEnablesettings.setAppCacheEnabled(true);
        webSettings.databaseEnabled = true// 开启
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.loadUrl(startUrl)
    }

    fun start(uri: String) {
        if (view == null) {
           startUrl  = uri
        } else {
            webView.loadUrl(uri)
        }
    }

    /**
     * JS调用android的方法
     * @param str
     * @return
     */
    @JavascriptInterface
    fun routerNative(url: String) {
        SchemaParser.parseRouter(requireContext(), childFragmentManager, url)
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_web
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.i("ansen", "是否有上一个页面:" + webView.canGoBack())
        if (webView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) { //点击返回按钮的时候判断有没有上一页
            webView.goBack() // goBack()表示返回webView的上一页面
            return true
        }
        return false
    }
}