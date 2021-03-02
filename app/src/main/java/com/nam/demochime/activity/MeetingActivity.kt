package com.nam.demochime.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.*
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileObserver
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.device.MediaDevice
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.amazonaws.services.chime.sdk.meetings.realtime.RealtimeObserver
import com.amazonaws.services.chime.sdk.meetings.session.*
import com.amazonaws.services.chime.sdk.meetings.utils.DefaultModality
import com.amazonaws.services.chime.sdk.meetings.utils.ModalityType
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.utils.CpuVideoProcessor
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.nam.demochime.JoinMeetingResponse
import com.nam.demochime.R
import com.nam.demochime.adapter.UserJoinedAdapter
import com.nam.demochime.customview.SlideCustomView
import com.nam.demochime.data.VideoCollectionTile
import com.nam.demochime.utils.AudioDeviceManager
import com.nam.demochime.utils.Convert
import com.nam.demochime.utils.GpuVideoProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView as DefaultVideoRenderView1

@Suppress("DEPRECATION")
class MeetingActivity : AppCompatActivity(), VideoTileObserver, RealtimeObserver,
    AudioVideoObserver, onItemBottomClickListener {
    private lateinit var audioVideo: AudioVideoFacade

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val currentRoster = mutableMapOf<String, RosterAttendee>()
    private val remoteVideoTileStates = mutableListOf<VideoCollectionTile>()
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private lateinit var cameraCaptureSource: CameraCaptureSource
    lateinit var gpuVideoProcessor: GpuVideoProcessor
    lateinit var cpuVideoProcessor: CpuVideoProcessor
    private lateinit var audioDeviceManager: AudioDeviceManager
    private val gson = Gson()
    private var meetingId: String? = null
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private lateinit var meetingSession: MeetingSession
    private val TAG: String = "XXXXX"
    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625
    private lateinit var imgVideoStatus: ImageView
    private lateinit var imgMuteStatus: ImageView
    private lateinit var localVideo: LinearLayout
    private lateinit var chimeContainer: RelativeLayout
    private lateinit var swipeCamera: ImageView
    private lateinit var btnSpeaker: ImageView
    private lateinit var exit_room: ImageView
    private lateinit var view: View
    private lateinit var view3: View
    private lateinit var userJoinedView: View
    private lateinit var rcUsersJoined: RecyclerView
    private lateinit var slideCustomView: SlideCustomView
    private lateinit var icMoveView: ImageView
    private lateinit var audioDevices: List<MediaDevice>
    private var checkVideoStatus = true
    private var checkMuteStatus = true
    private var isOpenListUser = false
    private var attendeeLocal: VideoCollectionTile? = null
    private var layoutParams: RecyclerView.LayoutParams? = null
    private var compareVideo: VideoCollectionTile? = null
    private val currentApiVersion = Build.VERSION.SDK_INT
    private var audioSelectDialog: AudioSelectDialog? = null
    private var userJoinedAdapter: UserJoinedAdapter? = null
    private val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_FULLSCREEN)

    private var chimeLocal: com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)
        configSession()
        initView()
        setUpFullScreen()
        audioVideo = meetingSession.audioVideo
        audioDeviceManager = AudioDeviceManager(audioVideo)
        subscribeListener()
        setUpLocalVideo()
        setUpChangeVideoStatus()
        setUpChangeMuteStatus()
        handleSwipeCamera()
        handleSelectAudioOutput()
        handleShowCurrentUsers()
    }

    private fun handleShowCurrentUsers() {
        slideCustomView.alpha = 0.5F
        userJoinedAdapter = UserJoinedAdapter(remoteVideoTileStates)
        rcUsersJoined.adapter = userJoinedAdapter
        icMoveView.setOnClickListener(View.OnClickListener {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            val moveViewXSize: Int = icMoveView.getMeasuredWidth()
            val slideXSize = slideCustomView.measuredWidth
            Log.d("xxxxxx", slideCustomView.layoutParams.height.toString())
            Log.d("xxxxxx", slideCustomView.layoutParams.width.toString())
            if (isOpenListUser) {
                slideCustomView.animate().x(width.toFloat() - Convert.convertDpToPixel(2f, this))
                    .duration = 300
                icMoveView.animate()
                    .x(width - moveViewXSize.toFloat() - Convert.convertDpToPixel(2f, this))
                    .duration = 300
                isOpenListUser = false
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_right_24)
            } else {
                icMoveView.animate().x(width - moveViewXSize - slideXSize.toFloat()).duration = 300
                slideCustomView.animate().x((width - slideXSize).toFloat()).duration = 300
                isOpenListUser = true
                icMoveView.setImageResource(R.drawable.ic_baseline_keyboard_arrow_left_24)
            }
        })
    }

    override fun onClickListener(position: Int) {
        audioVideo.chooseAudioDevice(audioDevices[position])
        audioSelectDialog?.dismiss()
    }

    private fun handleSelectAudioOutput() {
        audioDevices = audioVideo.listAudioDevices()
        btnSpeaker.setOnClickListener {
            audioSelectDialog = AudioSelectDialog(audioDevices, this)
            audioSelectDialog?.let {
                it.show(supportFragmentManager, "BottomDialog")
            }
        }

        audioDevices.forEach {
            Log.d(TAG, "handleSelectAudioOutput: ${it.label} : ${it.type}")
        }
        Log.d(TAG, "handleSelectAudioOutput: ")

    }

    private fun handleSwipeCamera() {
        swipeCamera.setOnClickListener {
            cameraCaptureSource.switchCamera()
        }
    }


    @Suppress("DEPRECATION")
    private fun setUpFullScreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        if (currentApiVersion >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = flags
            val decorView = window.decorView
            decorView.setOnSystemUiVisibilityChangeListener {
                if (it and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    decorView.systemUiVisibility = flags
                }
            }
        }
    }

    private fun initView() {
        imgMuteStatus = findViewById(R.id.mute_action_fab)
        imgVideoStatus = findViewById(R.id.local_video_action_fab)
        localVideo = findViewById(R.id.localVideo)
        chimeContainer = findViewById(R.id.chime_meeting_room_container)
        swipeCamera = findViewById(R.id.swipe_camera)
        btnSpeaker = findViewById(R.id.btnSpeaker)

        exit_room = findViewById(R.id.exit_room)
        view = layoutInflater.inflate(R.layout.chime_view, chimeContainer, false)
        view3 = layoutInflater.inflate(R.layout.local_video, localVideo, false)
        chimeContainer.addView(view)
        localVideo.addView(view3)
        chimeLocal = view3.findViewById(R.id.local_video)
        slideCustomView = findViewById(R.id.slideCustomView)
        icMoveView = findViewById(R.id.icMoveView)
        userJoinedView = layoutInflater.inflate(R.layout.view_users_joined, null, false)
        rcUsersJoined = userJoinedView.findViewById(R.id.rcUsersJoined)
        slideCustomView.addView(userJoinedView)
        icMoveView.alpha = 0.5f
    }


    private fun initChime() {
        if (compareVideo == null) {
            compareVideo = remoteVideoTileStates[0]
        }
        bind(remoteVideoTileStates[1], view.findViewById(R.id.chime))
        bind(remoteVideoTileStates[0], view3.findViewById(R.id.local_video))
    }

    private fun showLocal() {
        bind(remoteVideoTileStates[0], view.findViewById(R.id.chime))
    }

    private fun toggleCameraOn() {
        audioVideo.startLocalVideo(cameraCaptureSource)
        cameraCaptureSource.start()
        imgVideoStatus.setImageResource(R.drawable.video_enable)
        localVideo.visibility = View.VISIBLE
        chimeLocal!!.visibility = View.VISIBLE
    }


    private fun toggleCameraOff() {
        cameraCaptureSource.stop()
        audioVideo.stopLocalVideo()
        imgVideoStatus.setImageResource(R.drawable.video_disable)
        localVideo.visibility = View.GONE
        chimeLocal!!.visibility = View.GONE
    }

    private fun setUpLocalVideo() {
        gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
        cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
        audioVideo.startLocalVideo(cameraCaptureSource)
        cameraCaptureSource.start()
    }

    private fun subscribeListener() {
        audioVideo.addRealtimeObserver(this)
        audioVideo.addVideoTileObserver(this)
        audioVideo.addAudioVideoObserver(this)
        audioVideo.start()
        audioVideo.startRemoteVideo()
    }

    private fun bind(
        videoCollectionTile: VideoCollectionTile,
        videoRenderView: DefaultVideoRenderView1
    ) {
        videoCollectionTile.videoRenderView = videoRenderView
        audioVideo.bindVideoView(videoRenderView, videoCollectionTile.videoTileState.tileId)
    }

    private fun configSession() {
        meetingId = intent.getStringExtra(MainActivity.MEETING_ID_KEY) as String
        val response = intent.getStringExtra(MainActivity.MEETING_RESPONSE_KEY) as String
        val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
        val sessionConfig = MeetingSessionConfiguration(
            CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
            CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
            ::urlRewriter
        )
        val meetingSession = sessionConfig.let {
            Log.d("TAG", "Creating meeting session for meeting Id: $meetingId")
            DefaultMeetingSession(
                it,
                logger,
                applicationContext,
                // Note if the following isn't provided app will (as expected) crash if we use custom video source
                // since an EglCoreFactory will be internal created and will be using a different shared EGLContext.
                // However the internal default capture would work fine, since it is initialized using
                // that internally created default EglCoreFactory, and can be smoke tested by removing this
                // argument and toggling use of custom video source before starting video
                eglCoreFactory
            )

        }
        val surfaceTextureCaptureSourceFactory =
            DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
        cameraCaptureSource = DefaultCameraCaptureSource(
            applicationContext,
            logger,
            surfaceTextureCaptureSourceFactory
        )
        setMeetingSession(meetingSession)
    }

    override fun onVideoTileAdded(tileState: VideoTileState) {
        val videoCollectionTile = createVideoCollectionTile(tileState)
        val attendeeBefore = remoteVideoTileStates.find {
            it.videoTileState.attendeeId == videoCollectionTile.videoTileState.attendeeId
        }
        if (remoteVideoTileStates.isNotEmpty()) {
            attendeeLocal = remoteVideoTileStates[0]
        }

        if (attendeeBefore == null) {
            remoteVideoTileStates.add(videoCollectionTile)
        } else {
            remoteVideoTileStates.forEach {
                if (it.videoTileState.attendeeId == attendeeBefore.videoTileState.attendeeId) {
                    remoteVideoTileStates.replace(videoCollectionTile) { value ->
                        value == attendeeBefore
                    }
                }
            }
        }

        Log.d(TAG, "yyyy " + tileState.isLocalTile)
        Log.d(TAG, "zzzz " + remoteVideoTileStates.size)
        if (remoteVideoTileStates.size == 1) {
            showLocal()
        }
        if (remoteVideoTileStates.size == 2) {
            initChime()
            userJoinedAdapter?.notifyDataSetChanged()
        }
    }


    private fun setUpChangeMuteStatus() {
        imgMuteStatus.setOnClickListener {
            if (checkMuteStatus) {
                toggleMuteOff()
            } else {
                toggleMuteOn()
            }
            checkMuteStatus = !checkMuteStatus
        }
    }


    private fun setUpChangeVideoStatus() {
        imgVideoStatus.setOnClickListener {
            if (checkVideoStatus) {
                toggleCameraOff()
            } else {
                toggleCameraOn()
            }
            checkVideoStatus = !checkVideoStatus
        }
    }

    fun <T> List<T>.replace(newValue: T, block: (T) -> Boolean): List<T> {
        return map {
            if (block(it)) newValue else it
        }
    }

    override fun onVideoTilePaused(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTilePaused: ")
    }

    override fun onVideoTileRemoved(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileRemoved: ")

    }

    override fun onVideoTileResumed(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileResumed: ")
    }

    override fun onVideoTileSizeChanged(tileState: VideoTileState) {
        Log.d(TAG, "onVideoTileSizeChanged: ")
    }

    private fun createVideoCollectionTile(tileState: VideoTileState): VideoCollectionTile {
        val attendeeId = tileState.attendeeId
        val attendeeName = currentRoster[attendeeId]?.attendeeName ?: ""
        return VideoCollectionTile(attendeeName, tileState)
    }

    override fun onAttendeesDropped(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesDropped: ")
    }

    private fun toggleMuteOff() {
        audioVideo.realtimeLocalMute()
        imgMuteStatus.setImageResource(R.drawable.mic_disable)
    }

    private fun toggleMuteOn() {
        audioVideo.realtimeLocalUnmute()
        imgMuteStatus.setImageResource(R.drawable.mic_enable)
    }


    override fun onAttendeesJoined(attendeeInfo: Array<AttendeeInfo>) {
//        uiScope.launch {
//            mutex.withLock{
        attendeeInfo.forEach { (attendeeId, externalUserId) ->

            currentRoster.getOrPut(
                attendeeId,
                {
                    RosterAttendee(
                        attendeeId,
                        getAttendeeName(attendeeId, externalUserId)
                    )
                })
        }
        currentRoster.forEach { (id, roster) ->
            Log.d(TAG, "xxxxxxxxx " + roster.attendeeName)
        }
//            }
//        }
    }

    private val CONTENT_NAME_SUFFIX = "<<Content>>"
    private fun getAttendeeName(attendeeId: String, externalUserId: String): String {
        val attendeeName = externalUserId.split('#')[1]

        return if (DefaultModality(attendeeId).hasModality(ModalityType.Content)) {
            "$attendeeName $CONTENT_NAME_SUFFIX"
        } else {
            attendeeName
        }
    }

    override fun onAttendeesLeft(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesLeft: ")
    }

    override fun onAttendeesMuted(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesMuted: ")
    }

    override fun onAttendeesUnmuted(attendeeInfo: Array<AttendeeInfo>) {
        Log.d(TAG, "onAttendeesUnmuted: ")
    }

    override fun onSignalStrengthChanged(signalUpdates: Array<SignalUpdate>) {
        Log.d(TAG, "onSignalStrengthChanged: ")
    }

    override fun onVolumeChanged(volumeUpdates: Array<VolumeUpdate>) {
        Log.d(TAG, "onVolumeChanged: ")
    }

    private fun urlRewriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    fun setMeetingSession(meetingSession: MeetingSession) {
        this.meetingSession = meetingSession
    }

    override fun onAudioSessionCancelledReconnect() {
        Log.d(TAG, "onAudioSessionCancelledReconnect: ")
    }

    override fun onAudioSessionDropped() {
        Log.d(TAG, "onAudioSessionDropped: ")
    }

    override fun onAudioSessionStarted(reconnecting: Boolean) {
        Log.d(TAG, "onAudioSessionStarted: ")
    }

    override fun onAudioSessionStartedConnecting(reconnecting: Boolean) {
        Log.d(TAG, "onAudioSessionStartedConnecting: ")
    }

    override fun onAudioSessionStopped(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onAudioSessionStopped: ")
    }

    override fun onConnectionBecamePoor() {
        Log.d(TAG, "onConnectionBecamePoor: ")
    }

    override fun onConnectionRecovered() {
        Log.d(TAG, "onConnectionRecovered: ")
    }

    override fun onVideoSessionStarted(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onVideoSessionStarted: ")
        val audioDevices = meetingSession.audioVideo.listAudioDevices()
        audioDevices.forEach {
            Log.d(TAG, "Device type: ${it.type}, label: ${it.label}")
        }
        val myAudioDevice = meetingSession.audioVideo.listAudioDevices().filter {
            it.type != MediaDeviceType.OTHER
        }
        val device = myAudioDevice[0]
        meetingSession.audioVideo.chooseAudioDevice(device)
    }

    override fun onVideoSessionStartedConnecting() {
        Log.d(TAG, "onVideoSessionStartedConnecting: ")
    }

    override fun onVideoSessionStopped(sessionStatus: MeetingSessionStatus) {
        Log.d(TAG, "onVideoSessionStopped: ")
    }

}

data class RosterAttendee(
    val attendeeId: String,
    val attendeeName: String,
    val volumeLevel: VolumeLevel = VolumeLevel.NotSpeaking,
    val signalStrength: SignalStrength = SignalStrength.High,
    val isActiveSpeaker: Boolean = false
)

@Suppress("DEPRECATION")
class CustomBottomDialog(context: Context) : BottomSheetDialog(context) {

    override fun show() {
        window!!.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        super.show()
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        this.window!!.decorView.systemUiVisibility = uiOptions
        window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    override fun onStart() {
        super.onStart()
        if (window != null && window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window!!.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows =
                false
            val decorView = window!!.decorView
            decorView.systemUiVisibility =
                decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }
}

@Suppress("DEPRECATION")
class AudioSelectDialog(
    private val mediaDevices: List<MediaDevice>,
    private val onItemBottomClickListener: onItemBottomClickListener
) :
    BottomSheetDialogFragment() {
    private lateinit var speakersLv: ListView

    //    private lateinit var onItemBottomClickListener: onItemBottomClickListener
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_shet_dialog, container, false)
        speakersLv = view.findViewById(R.id.rcSpeakers)
        val adapter = ArrayAdapter<MediaDevice>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            mediaDevices
        )
        speakersLv.adapter = adapter
        speakersLv.setOnItemClickListener { _, _, position, _ ->
            onItemBottomClickListener.onClickListener(position)
        }
        return view
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        dialog?.window?.decorView?.systemUiVisibility = flags
        this.dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (view!!.parent.parent.parent as View).fitsSystemWindows = false

    }
}

interface onItemBottomClickListener {
    fun onClickListener(position: Int)
}
