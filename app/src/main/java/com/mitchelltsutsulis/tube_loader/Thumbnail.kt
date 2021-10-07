package com.mitchelltsutsulis.tube_loader

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Thumbnail(val source: String, val width: String,
                     val height: String): Parcelable
