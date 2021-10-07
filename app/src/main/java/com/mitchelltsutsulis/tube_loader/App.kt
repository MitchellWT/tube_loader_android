package com.mitchelltsutsulis.tube_loader

import android.app.Application
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView

class App: Application() {
    private lateinit var thumbnailCache: LruCache<String, Bitmap>

    override fun onCreate() {
        super.onCreate()

        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8

        thumbnailCache = LruCache<String, Bitmap>(cacheSize)
    }

    fun storeBitmap(bitmapKey: String, bitmap: Bitmap) {
        thumbnailCache.put(bitmapKey, bitmap)
    }

    fun loadBitmap(bitmapKey: String, imageView: ImageView) {
        val bitmap: Bitmap? = thumbnailCache.get(bitmapKey)

        bitmap?.let {
            imageView.setImageBitmap(it)
        }
    }
}