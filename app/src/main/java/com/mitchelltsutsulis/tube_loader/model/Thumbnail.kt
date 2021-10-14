package com.mitchelltsutsulis.tube_loader.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Thumbnail(val source: String, val width: String = "480",
                     val height: String = "360"): Parcelable
