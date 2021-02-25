package com.nam.demochime.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nam.demochime.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private var meetingIdEditText: EditText? = null
    private var nameEditText: EditText? = null
    private val MEETING_REGION = "us-east-1"
    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    private val WEBRTC_PERMISSION_REQUEST_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setUpView()
    }

    private fun setUpView() {
        meetingIdEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        val imageButton = findViewById<ImageButton>(R.id.buttonContinue);
        imageButton?.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val response = joinMeeting(
                    " https://calling-chime-demo.dev.calling.fun/",
                    meetingIdEditText?.text.toString(),
                    nameEditText?.text.toString()
                )
                if (response != null) {
                    if (hasPermissionsAlready()) {
                        val intent = Intent(applicationContext, JoinMeetingActivity::class.java)
                        intent.putExtra(MEETING_RESPONSE_KEY, response)
                        intent.putExtra(MEETING_ID_KEY, meetingIdEditText?.text.toString())
                        intent.putExtra(NAME_KEY, nameEditText?.text.toString())
                        startActivity(intent)
                    } else {
                        ActivityCompat.requestPermissions(this@MainActivity, WEBRTC_PERM, WEBRTC_PERMISSION_REQUEST_CODE)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    return
                }
                setUpView()
            }
        }
    }

    private fun hasPermissionsAlready(): Boolean {
        return WEBRTC_PERM.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun encodeURLParam(string: String?): String {
        return URLEncoder.encode(string, "utf-8")
    }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ): String? {
        return withContext(Dispatchers.IO) {
            val url = if (meetingUrl.endsWith("/")) meetingUrl else "$meetingUrl/"
            val serverUrl =
                URL(
                    "${url}join?title=${encodeURLParam(meetingId)}&name=${encodeURLParam(
                        attendeeName
                    )}&region=${encodeURLParam(MEETING_REGION)}"
                )

            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 201) {
                        response.toString()
                    } else {
                        Log.d("TAG", "Unable to join meeting. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                Log.d("TAG", "There was an exception while joining the meeting: $exception")
                null
            }
        }
    }

    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
    }
}