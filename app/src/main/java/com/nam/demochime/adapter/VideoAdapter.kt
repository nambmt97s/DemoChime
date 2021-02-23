package com.nam.demochime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.AudioVideoFacade
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.DefaultVideoRenderView
import com.amazonaws.services.chime.sdk.meetings.audiovideo.video.capture.CameraCaptureSource
import com.amazonaws.services.chime.sdk.meetings.device.MediaDeviceType
import com.nam.demochime.R
import com.nam.demochime.data.VideoCollectionTile

class VideoAdapter(
    private val videoCollectionTiles: Collection<VideoCollectionTile>,
    private val audioVideoFacade: AudioVideoFacade,
    private val cameraCaptureSource: CameraCaptureSource
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return ViewHolder(view = view, audioVideoFacade = audioVideoFacade, cameraCaptureSource = cameraCaptureSource)
    }

    override fun getItemCount() = videoCollectionTiles.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videoCollectionTiles.elementAt(position)
        holder.bind(video)
    }

    class ViewHolder(private val view: View, private val audioVideoFacade: AudioVideoFacade, private val cameraCaptureSource: CameraCaptureSource) :
        RecyclerView.ViewHolder(view) {

        private val videoRenderView = view.findViewById<DefaultVideoRenderView>(R.id.video)

        fun bind(videoCollectionTile: VideoCollectionTile) {
            videoCollectionTile.videoRenderView = videoRenderView

            audioVideoFacade.bindVideoView(
                videoRenderView,
                videoCollectionTile.videoTileState.tileId
            )
//            updateLocalVideoMirror()
        }
        private fun updateLocalVideoMirror() {
            videoRenderView.mirror =
                    // If we are using internal source, base mirror state off that device type
                (audioVideoFacade.getActiveCamera()?.type == MediaDeviceType.VIDEO_FRONT_CAMERA ||
                        // Otherwise (audioVideo.getActiveCamera() == null) use the device type of our external/custom camera capture source
                        (audioVideoFacade.getActiveCamera() == null && cameraCaptureSource.device?.type == MediaDeviceType.VIDEO_FRONT_CAMERA))
        }
    }
}