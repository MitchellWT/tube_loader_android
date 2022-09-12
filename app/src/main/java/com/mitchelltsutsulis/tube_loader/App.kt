package com.mitchelltsutsulis.tube_loader

import android.app.Application
import java.util.*

class App: Application() {
    lateinit var basicAuthStr: String

    override fun onCreate() {
        super.onCreate()
        basicAuthStr = Base64.getEncoder().encodeToString(
            "${getString(R.string.username)}:${getString(R.string.password)}".toByteArray()
        )
    }
}
