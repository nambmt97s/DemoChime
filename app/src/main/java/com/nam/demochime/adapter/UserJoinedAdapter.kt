package com.nam.demochime.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nam.demochime.R
import com.nam.demochime.data.VideoCollectionTile

class UserJoinedAdapter(private val users: List<VideoCollectionTile>) :
    RecyclerView.Adapter<UserJoinedAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_user_joined, parent, false)
        )
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var imgAvatarUser: ImageView = itemView.findViewById(R.id.imgAvatarUser)
        private var tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        fun bind(user: VideoCollectionTile) {
            tvUserName.text = user.attendeeName + "Hello"
        }
    }
}