package com.mitchelltsutsulis.tube_loader

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class App : Application() {
    private lateinit var masterKeyAlias: String
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()

        masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "tube_loader_private",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadData(
        authToken: String,
        youtubeToken: String,
        serverScheme: String,
        serverAuthority: String
    ) = prefs.edit {
        putString("auth_token", authToken)
        putString("youtube_token", youtubeToken)
        putString("server_scheme", serverScheme)
        putString("server_authority", serverAuthority)
    }

    fun deleteData() = prefs.edit {
        putString("auth_token", "")
        putString("youtube_token", "")
        putString("server_scheme", "")
        putString("server_authority", "")
    }

    fun getAuthToken() = prefs.getString("auth_token", "") ?: ""
    fun getYoutubeToken() = prefs.getString("youtube_token", "") ?: ""
    fun getServerScheme() = prefs.getString("server_scheme", "") ?: ""
    fun getServerAuthority() = prefs.getString("server_authority", "")  ?: ""
}
