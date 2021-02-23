package com.nam.demochime.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultCameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.DefaultSurfaceTextureCaptureSourceFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.DefaultEglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl.EglCoreFactory
import com.amazonaws.services.chime.sdk.meetings.session.*
import com.amazonaws.services.chime.sdk.meetings.utils.logger.ConsoleLogger
import com.amazonaws.services.chime.sdk.meetings.utils.logger.LogLevel
import com.amazonaws.services.chime.sdkdemo.utils.CpuVideoProcessor
import com.google.gson.Gson
import com.nam.demochime.JoinMeetingResponse
import com.nam.demochime.R
import com.nam.demochime.utils.GpuVideoProcessor

class JoinMeetingActivity : AppCompatActivity() {

    private lateinit var meetingId: String
    private lateinit var name: String
    private val gson = Gson()
    private val logger = ConsoleLogger(LogLevel.DEBUG)
    private val eglCoreFactory: EglCoreFactory = DefaultEglCoreFactory()
    private lateinit var meetingSession: MeetingSession
    lateinit var cameraCaptureSource: CameraCaptureSource
    lateinit var gpuVideoProcessor: GpuVideoProcessor
    lateinit var cpuVideoProcessor: CpuVideoProcessor
    private var videoPreview: DefaultVideoRenderView? = null
    private var btnJoin: Button? = null
    private var response:String ?= null


    private val VIDEO_ASPECT_RATIO_16_9 = 0.5625

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_meeting)
        meetingId = intent.getStringExtra(MainActivity.MEETING_ID_KEY) as String
        name = intent.getStringExtra(MainActivity.NAME_KEY) as String
        response = intent.getStringExtra(MainActivity.MEETING_RESPONSE_KEY) as String
        val joinMeetingResponse = gson.fromJson(response, JoinMeetingResponse::class.java)
        val sessionConfig = MeetingSessionConfiguration(
            CreateMeetingResponse(joinMeetingResponse.joinInfo.meetingResponse.meeting),
            CreateAttendeeResponse(joinMeetingResponse.joinInfo.attendeeResponse.attendee),
            ::urlRewriter
        )
        val meetingSession = sessionConfig?.let {
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

        if (meetingSession == null) {
            Toast.makeText(
                applicationContext,
                "Error",
                Toast.LENGTH_LONG
            ).show()
            finish()
        } else {
            setMeetingSession(meetingSession)
        }
        val surfaceTextureCaptureSourceFactory =
            DefaultSurfaceTextureCaptureSourceFactory(logger, eglCoreFactory)
        cameraCaptureSource = DefaultCameraCaptureSource(
            applicationContext,
            logger,
            surfaceTextureCaptureSourceFactory
        )
        cpuVideoProcessor = CpuVideoProcessor(logger, eglCoreFactory)
        gpuVideoProcessor = GpuVideoProcessor(logger, eglCoreFactory)
        videoPreview = findViewById(R.id.videoPreview)
        videoPreview?.let {
            val displayMetrics = this.resources.displayMetrics

            val width = if (isLandscapeMode(this)) displayMetrics.widthPixels / 2 else displayMetrics.widthPixels
            val height = (width * VIDEO_ASPECT_RATIO_16_9).toInt()
            it.layoutParams.width = width
            it.layoutParams.height = height
            it.logger = logger
            it.init(eglCoreFactory)
            cameraCaptureSource.addVideoSink(it)
            videoPreview = it
        }
        cameraCaptureSource.start()
        btnJoin = findViewById(R.id.btnJoin)
        btnJoin?.setOnClickListener {
            val intent = Intent(this, MeetingActivity::class.java)
            intent.putExtra(MainActivity.MEETING_ID_KEY,meetingId)
            intent.putExtra(MainActivity.MEETING_RESPONSE_KEY,response)
            startActivity(intent)
        }
    }



    fun isLandscapeMode(context: Context?): Boolean {
        return context?.let { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE } ?: false
    }
    private fun urlRewriter(url: String): String {
        // You can change urls by url.replace("example.com", "my.example.com")
        return url
    }

    fun setMeetingSession(meetingSession: MeetingSession) {
        this.meetingSession = meetingSession
    }

    fun getSession(): MeetingSession {
        return this.meetingSession
    }
}

