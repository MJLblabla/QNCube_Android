package com.qiniudemo.baseapp.web

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.qiniu.baseapp.R
import com.qiniudemo.baseapp.BaseActivity

class WebViewActivity : BaseActivity() {

    private val webFragment: WebFragment by lazy { WebFragment() }

    override fun isToolBarEnable(): Boolean {
        return false
    }

    companion object {
        fun start(url: String, context: Context) {
            val i = Intent(context, WebViewActivity::class.java)
            i.putExtra("url", url)
            context.startActivity(i)
        }
    }

    private var url = ""
    override fun initViewData() {
        webFragment.webViewTitleCall = {
            setToolbarTitle(title)
        }
        url = intent.getStringExtra("url") ?: ""
        val trans = supportFragmentManager.beginTransaction()
        trans.replace(R.id.webContainer, webFragment)
        trans.commit()
        webFragment.start(url)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (webFragment.onKeyDown(keyCode, event)) { //点击返回按钮的时候判断有没有上一页
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_web_view
    }
}