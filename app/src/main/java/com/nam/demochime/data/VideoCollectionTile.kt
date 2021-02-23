package com.nam.demochime.data

import androidx.constraintlayout.widget.ConstraintLayout
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.VideoTileState

data class VideoCollectionTile(
    val attendeeName: String,
    val videoTileState: VideoTileState
) {
    var videoRenderView: DefaultVideoRenderView? = null
    var pauseMessageView: ConstraintLayout? = null

    fun setRenderViewVisibility(visibility: Int) {
        videoRenderView?.visibility = visibility
    }

    fun setPauseMessageVisibility(visibility: Int) {
        pauseMessageView?.visibility = visibility
    }
}