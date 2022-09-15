package com.mitchelltsutsulis.tube_loader

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import java.io.IOException
import java.util.*

class LoginActivity : AppCompatActivity() {
    private val httpClient = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val usernameView = findViewById<TextView>(R.id.username)
        val passwordView = findViewById<TextView>(R.id.password)
        val serverView = findViewById<TextView>(R.id.server)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = usernameView.text.toString()
            val password = passwordView.text.toString()
            val serverParts = serverView.text.toString().split("://")
            val serverScheme = serverParts[0]
            val serverAuthority = serverParts[1]
            loginButton.text = getString(R.string.loading)
            login(username, password, serverScheme, serverAuthority)
        }
    }

    private fun login(
        username: String,
        password: String,
        serverScheme: String,
        serverAuthority: String
    ) {
        val authToken = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
        val url = Uri.Builder()
            .scheme(serverScheme)
            .encodedAuthority(serverAuthority)
            .appendPath("token")
            .build()
            .toString()
        val req = Request.Builder()
            .get()
            .url(url)
            .addHeader("Authorization", "Basic $authToken")
            .build()
        httpClient.newCall(req)
            .enqueue(LoginCallback(this, authToken, serverScheme, serverAuthority))
    }

    class LoginCallback(
        private val loginAct: LoginActivity,
        private val authToken: String,
        private val serverScheme: String,
        private val serverAuthority: String
    ) : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.i("GET TOKEN FAIL", e.message.toString())
            loginAct.findViewById<Button>(R.id.login_button).text =
                loginAct.getString(R.string.login)
            Snackbar.make(
                loginAct.findViewById(R.id.content),
                "Unable to get token! Please check your connection!",
                Snackbar.LENGTH_SHORT
            ).setAnchorView(loginAct.findViewById(R.id.login_button)).show()
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                Log.i(
                    "GET TOKEN FAIL",
                    "Status code: ${response.code}, message: ${response.message}"
                )
                Snackbar.make(
                    loginAct.findViewById(R.id.content),
                    "Unable to get token! Please check your connection!",
                    Snackbar.LENGTH_SHORT
                ).setAnchorView(
                    loginAct.findViewById(R.id.bottom_navigation_bar)
                ).show()
                return
            }
            loginAct.findViewById<Button>(R.id.login_button).text =
                loginAct.getString(R.string.login)
            val youtubeToken = loginAct.objectMapper
                .readValue<Map<String, String>>(response.body?.string() ?: "")
                .getValue("key")
                .toString()
            loginAct.saveAndExit(authToken, youtubeToken, serverScheme, serverAuthority)
        }
    }

    private fun saveAndExit(
        authToken: String,
        youtubeToken: String,
        serverScheme: String,
        serverAuthority: String
    ) {
        (application as App).loadData(authToken, youtubeToken, serverScheme, serverAuthority)
        finish()
    }
}
