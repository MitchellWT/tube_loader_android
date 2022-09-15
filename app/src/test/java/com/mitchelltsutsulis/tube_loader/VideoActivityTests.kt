package com.mitchelltsutsulis.tube_loader

import android.util.Log
import io.mockk.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import java.io.IOException

class VideoActivityTests {
    @Test
    fun `test adding to system callback success`() {
        mockkStatic(Log::class)
        val mockVideoActivity = mockk<VideoActivity>()
        val addSystemCallback = VideoActivity.AddSystemCallback(mockVideoActivity)
        val req = Request.Builder()
            .url("https://cool_site.com")
            .build()
        val res = Response.Builder()
            .protocol(Protocol.HTTP_1_1)
            .request(req)
            .code(200)
            .message("")
            .build()
        val call = OkHttpClient().newCall(req)

        every { Log.i(any(), any()) } returns 0
        every { mockVideoActivity.exit(any()) } just Runs

        addSystemCallback.onResponse(call, res)
        verify { mockVideoActivity.exit(200) }

        unmockkStatic(Log::class)
        unmockkAll()
    }

    @Test
    fun `test adding to system callback fail`() {
        mockkStatic(Log::class)
        val mockVideoActivity = mockk<VideoActivity>()
        val addSystemCallback = VideoActivity.AddSystemCallback(mockVideoActivity)
        val req = Request.Builder()
            .url("https://cool_site.com")
            .build()
        val call = OkHttpClient().newCall(req)

        every { Log.i(any(), any()) } returns 0
        every { mockVideoActivity.exit(any()) } just Runs

        addSystemCallback.onFailure(call, IOException())
        verify { mockVideoActivity.exit(500) }

        unmockkStatic(Log::class)
        unmockkAll()
    }
}