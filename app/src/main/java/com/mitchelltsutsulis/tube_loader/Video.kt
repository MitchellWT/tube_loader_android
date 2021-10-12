package com.mitchelltsutsulis.tube_loader

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Video(val videoId: String, val title: String,
                 val thumbnail: Thumbnail,
                 val queued: Boolean = false,
                 val downloaded: Boolean = false): Parcelable
