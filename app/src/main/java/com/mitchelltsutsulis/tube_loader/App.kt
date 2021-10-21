package com.mitchelltsutsulis.tube_loader

import android.app.Application
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.ImageView

class App: Application() {
    private lateinit var thumbnailCache: LruCache<String, Bitmap>

    override fun onCreate() {
        super.onCreate()
        // Set cache size
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        // Create cache from above size
        thumbnailCache = LruCache<String, Bitmap>(cacheSize)
    }

    // Storages a bitmap with a key value pair (String -> Bitmap)
    fun storeBitmap(bitmapKey: String, bitmap: Bitmap) {
        thumbnailCache.put(bitmapKey, bitmap)
    }

    // Loads a imageView with a bitmap using a key
    fun loadBitmap(bitmapKey: String, imageView: ImageView) {
        val bitmap: Bitmap? = thumbnailCache.get(bitmapKey)
        // Only sets bitmap If bitmap was found
        bitmap?.let {
            imageView.setImageBitmap(it)
        }
    }

    // Checks If a bitmap exists in the bitmap cache
    fun checkBitmap(bitmapKey: String): Boolean {
        val bitmap: Bitmap? = thumbnailCache.get(bitmapKey)
        var found = false
        // Only set to true If found
        bitmap?.let {
            found = true
        }

        return found
    }
}