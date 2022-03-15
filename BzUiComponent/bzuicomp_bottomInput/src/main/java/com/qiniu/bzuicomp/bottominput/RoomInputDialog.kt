package com.qiniu.bzuicomp.bottominput

import android.content.DialogInterface
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import com.hapi.happy_dialog.FinalDialogFragment
import com.hapi.ut.SoftInputUtil
import kotlinx.android.synthetic.main.dialog_room_input.*


/**
 * 底部输入框
 */
class RoomInputDialog( val style: Int = 1) : FinalDialogFragment() {

    companion object {
        const val type_text = 1
        const val type_danmu = 2
    }

    init {
        applyGravityStyle(Gravity.BOTTOM)
        applyDimAmount(0f)
    }

    /**
     * 发消息拦截回调
     */
    var sendPubCall: ((msg: String) -> Unit)? = null
    override fun getViewLayoutId(): Int {
        return R.layout.dialog_room_input
    }

    override fun init() {
        val viewInput = when (style) {
            1 -> {
                flInput.setBackgroundColor(Color.parseColor("#eeeeee"))
                LightInputView(requireContext())
            }
            2 -> {
                flInput.setBackgroundColor(Color.parseColor("#000000"))
                DarkInputView(requireContext())
            }
            else -> {
                RoomInputView(requireContext())
            }
        }
        flInput.addView(
            viewInput,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        viewInput.attachActivity(requireActivity())
        viewInput.sendPubCall = {
            sendPubCall?.invoke(it)
            dismiss()
        }
        viewInput.requestEditFocus()
    }
}