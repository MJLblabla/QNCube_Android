package com.qiniudemo.module.interview.room


import android.Manifest
import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.hapi.baseframe.dialog.FinalDialogFragment
import com.hipi.vm.lazyVm
import com.niucube.absroom.seat.ScreenMicSeat
import com.niucube.mutabletrackroom.MicSeatListener
import com.qiniu.router.RouterConstant
import com.qiniudemo.baseapp.BaseActivity
import com.qiniudemo.baseapp.ext.asToast
import com.qiniu.bzcomp.user.UserInfoManager
import com.niucube.qrtcroom.screencapture.ScreenMicSeatListener
import com.niucube.comproom.RoomEntity
import com.niucube.comproom.RoomLifecycleMonitor
import com.niucube.comproom.RoomManager
import com.niucube.comproom.provideMeId
import com.niucube.qrtcroom.screencapture.ScreenCapturePlugin
import com.niucube.mutabletrackroom.MutableMicSeat
import com.niucube.qrtcroom.screencapture.QScreenTrackParams
import com.qiniudemo.baseapp.KeepLight
import com.qiniudemo.baseapp.widget.CommonTipDialog
import com.qiniudemo.module.interview.R
import com.qiniudemo.module.interview.been.InterviewRoomModel
import com.qiniudemo.module.interview.databinding.InterviewActivityInterviewRoomBinding
import com.tbruyelle.rxpermissions2.RxPermissions

@Route(path = RouterConstant.Interview.InterviewRoom)
class InterviewRoomActivity : BaseActivity<InterviewActivityInterviewRoomBinding>() {

    /**
     * 房间业务
     */
    private val mInterviewRoomVm by lazyVm<InterviewRoomVm>()

    /**
     * 面试ID
     */
    @Autowired
    @JvmField
    var interviewId = ""

    /**
     * 面试房间引擎
     */
    private val mInterviewRoom by lazy {
        mInterviewRoomVm.mInterviewRoom
    }

    private lateinit var bigSurfaceFront: InterviewSurfaceView
    private lateinit var bigSurfaceBack: InterviewSurfaceView
    private lateinit var bigSurfaceFrontContainer: FrameLayout
    private lateinit var bigSurfaceBackContainer: FrameLayout

    /**
     * 麦位监听
     */
    private val mTrackSeatListener by lazy {
        object : MicSeatListener {
            override fun onUserSitDown(micSeat: MutableMicSeat) {
                if (!micSeat.isMySeat(UserInfoManager.getUserId())) {
                    checkIsRoomOnlyMe()
                }
                //我的麦位
                if (micSeat.isMySeat(UserInfoManager.getUserId())) {
                    binding.smallSurfaceView.onSeatDown(mInterviewRoom, micSeat)
                } else {
                    bigSurfaceBack.onSeatDown(mInterviewRoom, micSeat)
                }
            }

            override fun onUserSitUp(micSeat: MutableMicSeat, isOffLine: Boolean) {
                if (micSeat.isMySeat(UserInfoManager.getUserId())) {
                    binding.smallSurfaceView.onSeatLeave(mInterviewRoom, micSeat)
                } else {
                    bigSurfaceBack.onSeatLeave(mInterviewRoom, micSeat)
                }
                //如果是房主（面试官）
                checkIsRoomOnlyMe()
            }

            override fun onCameraStatusChanged(micSeat: MutableMicSeat) {

                if (micSeat.isMySeat(UserInfoManager.getUserId())) {
                    binding.smallSurfaceView.onTrackStatusChange(mInterviewRoom, micSeat)
                } else {
                    bigSurfaceBack.onTrackStatusChange(mInterviewRoom, micSeat)
                }
            }

            override fun onMicAudioStatusChanged(micSeat: MutableMicSeat) {
                if (micSeat.isMySeat(UserInfoManager.getUserId())) {
                    binding.smallSurfaceView.onTrackStatusChange(mInterviewRoom, micSeat)
                } else {
                    bigSurfaceBack.onTrackStatusChange(mInterviewRoom, micSeat)
                }
            }
        }
    }

    //屏幕共享监听
    private var mScreenMicSeatListener = object : ScreenMicSeatListener {
        override fun onScreenMicSeatAdd(seat: ScreenMicSeat) {
            //屏幕轨道
            bigSurfaceFront.onScreenSeatAdd(mInterviewRoom, seat)
            onScreenTrackOn(seat)
            binding.btScree.isSelected = true
        }

        override fun onScreenMicSeatRemove(seat: ScreenMicSeat) {
            bigSurfaceFront.onScreenSeatRemoved(mInterviewRoom, seat)
            onScreenTrackOff()
            binding.btScree.isSelected = false
            checkIsRoomOnlyMe()
        }
    }

    /**
     * 房间生命周期监听
     */
    private val roomMonitor = object : RoomLifecycleMonitor {
        override fun onRoomJoined(roomEntity: RoomEntity) {
            super.onRoomJoined(roomEntity)
            val roomToken = (roomEntity as InterviewRoomModel)
            roomToken.allUserList?.forEach {
                if (it.accountId == roomEntity.provideMeId()) {
                    binding.smallSurfaceView.setUserInfo(it)
                } else {
                    bigSurfaceBack.setUserInfo(it)
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    override fun init() {
        lifecycle.addObserver(KeepLight(this))
        RoomManager.addRoomLifecycleMonitor(roomMonitor)
        // 轨道回调监听
        mInterviewRoom.addMicSeatListener(mTrackSeatListener)
        mInterviewRoom.screenShareManager.addScreenMicSeatListener(mScreenMicSeatListener)
        val trans = supportFragmentManager.beginTransaction()
        trans.replace(R.id.flCover, RoomCover())
        trans.commit()
        binding.trackVp.adapter = BigSurfaceViewAdapter()
        binding.trackVp.offscreenPageLimit = 2
        onScreenTrackOff()
        binding.smallSurfaceView.setIsTop(true)
        binding.smallSurfaceViewParent.setOnClickListener {
            swapSurfaceView()
        }
        binding.smallSurfaceViewParent.post {
            checkIsRoomOnlyMe()
        }

        lifecycle.addObserver(binding.smallSurfaceView)
        binding.trackVp.post {
            lifecycle.addObserver(bigSurfaceBack)
            lifecycle.addObserver(bigSurfaceFront)
        }

        binding.btScree.setOnClickListener {
            if (RoomManager.mCurrentRoom?.isJoined == false) {
                return@setOnClickListener
            }
            if (binding.btScree.isSelected) {
                if (bigSurfaceFront.mTargetSeat?.uid == UserInfoManager.getUserId()) {
                    mInterviewRoom.screenShareManager.unPubLocalScreenTrack()
                    binding.btScree.isSelected = false
                } else {

                }
            } else {
                if (bigSurfaceFront.mTargetSeat != null) {
                    "正在共享屏幕".asToast()
                }

                mInterviewRoom.screenShareManager.pubLocalScreenTrackWithPermissionCheck(
                    this, object : ScreenCapturePlugin.ScreenCaptureListener {
                        override fun onSuccess() {
                            binding.btScree.isSelected = true
                        }

                        override fun onError(code: Int, msg: String?) {}
                    },
                    QScreenTrackParams()
                )
            }
        }

        binding.trackVp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.rgIndicator.check(R.id.rbFront)
                    1 -> binding.rgIndicator.check(R.id.rbBack)
                }
            }
        })
        binding.rgIndicator.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFront -> {
                    if (binding.trackVp.currentItem != 0) {
                        binding.trackVp.currentItem = 0
                    }
                }
                R.id.rbBack -> {
                    if (binding.trackVp.currentItem != 1) {
                        binding.trackVp.currentItem = 1
                    }
                }
            }
        }
        RxPermissions(this)
            .request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
            )
            .subscribe {
                if (it) {
                    //进入房间
                    mInterviewRoomVm.enterRoom(interviewId)
                } else {
                    CommonTipDialog.TipBuild()
                        .setContent("请开启必要权限")
                        .setListener(object : FinalDialogFragment.BaseDialogListener() {
                            override fun onDismiss(dialog: DialogFragment) {
                                super.onDismiss(dialog)
                                finish()
                            }
                        })
                        .build()
                        .show(supportFragmentManager, "CommonTipDialog")
                }
            }
    }

    //交换两个视频
    private fun swapSurfaceView() {
        val smallView: InterviewSurfaceView =
            binding.smallSurfaceViewParent.getChildAt(0) as InterviewSurfaceView
        val bigView: InterviewSurfaceView = if (binding.trackVp.currentItem == 0)
            bigSurfaceFrontContainer.getChildAt(0) as InterviewSurfaceView
        else
            bigSurfaceBackContainer.getChildAt(0) as InterviewSurfaceView
        val lpBig = bigView.layoutParams
        val lpSmall = smallView.layoutParams
        smallView.layoutParams = lpBig
        bigView.layoutParams = lpSmall
        val parentSmall = smallView.parent as ViewGroup
        parentSmall.removeView(smallView)
        val parentBig = bigView.parent as ViewGroup
        parentBig.removeView(bigView)
        parentBig.addView(smallView)
        parentSmall.addView(bigView)
        bigView.setIsTop(true)
        smallView.setIsTop(false)
    }

    /**
     * 屏幕采集轨道打开
     */
    private fun onScreenTrackOn(targetSeat: ScreenMicSeat) {
        if (!targetSeat.isMySeat(UserInfoManager.getUserId())) {
            binding.trackVp.currentItem = 0
            bigSurfaceFront.setCover(0)
            if (!targetSeat.isMySeat(UserInfoManager.getUserId())) {
                binding.trackVp.isUserInputEnabled = true
                binding.rgIndicator.visibility = View.VISIBLE
            } else {
                binding.trackVp.isUserInputEnabled = false
                binding.rgIndicator.visibility = View.GONE
            }
        }
    }

    /**
     * 屏幕采集轨道关闭
     */
    private fun onScreenTrackOff() {
        binding.trackVp.currentItem = 1
        binding.trackVp.isUserInputEnabled = false
        binding.rgIndicator.visibility = View.GONE
    }

    private fun resetSwap() {
        val smv = binding.smallSurfaceView
        binding.smallSurfaceViewParent.removeViewAt(0)
        bigSurfaceBackContainer.removeViewAt(0)
        bigSurfaceFrontContainer.removeViewAt(0)
        binding.smallSurfaceViewParent.addView(smv)
        bigSurfaceBackContainer.addView(bigSurfaceBack)
        bigSurfaceFrontContainer.addView(bigSurfaceFront)
        binding.smallSurfaceView.setIsTop(true)
        bigSurfaceBack.setIsTop(false)
        bigSurfaceFront.setIsTop(false)

    }

    private fun checkIsRoomOnlyMe() {

        var isAllMe = true
        mInterviewRoom.mMicSeats.forEach {
            if (!it.isMySeat(UserInfoManager.getUserId())) {
                isAllMe = false
            }
        }
        if (mInterviewRoom.mMicSeats.isEmpty() || isAllMe) {

            if (bigSurfaceBackContainer.getChildAt(0) == binding.smallSurfaceView
                && bigSurfaceFrontContainer.getChildAt(0) == bigSurfaceFront
                && binding.smallSurfaceViewParent.getChildAt(0) == bigSurfaceBack
            ) {
                binding.smallSurfaceView.visibility = View.VISIBLE
                bigSurfaceFront.visibility = View.GONE
                bigSurfaceBack.visibility = View.GONE
                return
            }
            if (bigSurfaceBackContainer.getChildAt(0) != bigSurfaceBack
                || bigSurfaceFrontContainer.getChildAt(0) != bigSurfaceFront
                || binding.smallSurfaceViewParent.getChildAt(0) != binding.smallSurfaceView

            ) {
                resetSwap()
            }
            swapSurfaceView()
            binding.smallSurfaceView.visibility = View.VISIBLE
            bigSurfaceFront.visibility = View.GONE
            bigSurfaceBack.visibility = View.GONE
        } else {
            resetSwap()
            binding.smallSurfaceView.visibility = View.VISIBLE
            bigSurfaceFront.visibility = View.VISIBLE
            bigSurfaceBack.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RoomManager.removeRoomLifecycleMonitor(roomMonitor)
        mInterviewRoom.closeRoom()
    }

    override fun isToolBarEnable(): Boolean {
        return false
    }


    //安卓重写返回键事件
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK
            && RoomManager.mCurrentRoom?.isJoined == true
        ) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        if (RoomManager.mCurrentRoom?.isJoined == true) {
            streamerSwitchToBackstage()
        }
    }

    override fun onResume() {
        super.onResume()
        if (RoomManager.mCurrentRoom?.isJoined == true) {
            streamerBackToLiving()
        }
    }

    private fun streamerSwitchToBackstage() {
        //设置后台推流背景
//        val image = QNImage(this)
//        image.setResourceId(R.drawable.interview_pause_publish)
//        mInterviewRoom.pushCameraTrackWithImage(image)
    }

    private fun streamerBackToLiving() {
        //   mInterviewRoom.pushCameraTrackWithImage(null)
    }

    inner class BigSurfaceViewAdapter :
        RecyclerView.Adapter<BigSurfaceViewAdapter.ViewPagerViewHolder>() {

        inner class ViewPagerViewHolder(private val interviewSurfaceView: View) :
            RecyclerView.ViewHolder(interviewSurfaceView) {
            var flBigTrackContainer =
                interviewSurfaceView.findViewById<FrameLayout>(R.id.flBigTrackContainer)
            var bigSurfaceView =
                interviewSurfaceView.findViewById<InterviewSurfaceView>(R.id.bigSurfaceView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
            return ViewPagerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.interview_item_surface_track, parent, false)
            )
        }

        override fun getItemCount(): Int {
            return 2
        }

        override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
            holder.bigSurfaceView.setIsTop(false)
            if (position == 0) {
                bigSurfaceFrontContainer = holder.flBigTrackContainer
                bigSurfaceFront = holder.bigSurfaceView
            } else {
                bigSurfaceBackContainer = holder.flBigTrackContainer
                bigSurfaceBack = holder.bigSurfaceView
            }
        }
    }
}