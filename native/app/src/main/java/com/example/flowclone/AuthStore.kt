package com.example.flowclone

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 登录态/会话存储（自建 Server 模式） */
object AuthStore {
    private var prefs: SharedPreferences? = null

    var token by mutableStateOf<String?>(null)
        private set
    var refreshToken by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf<String?>(null)
        private set
    var userId by mutableStateOf<String?>(null)
        private set
    var credits by mutableStateOf(0)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("flow_auth", Context.MODE_PRIVATE)
        token = prefs?.getString("token", null)
        refreshToken = prefs?.getString("refreshToken", null)
        email = prefs?.getString("email", null)
        userId = prefs?.getString("userId", null)
        credits = prefs?.getInt("credits", 0) ?: 0
        isLoggedIn = !token.isNullOrBlank()
    }

    fun save(token: String, refresh: String?, email: String?, userId: String?, credits: Int) {
        this.token = token
        this.refreshToken = refresh
        this.email = email
        this.userId = userId
        this.credits = credits
        isLoggedIn = true
        prefs?.edit()
            ?.putString("token", token)
            ?.putString("refreshToken", refresh)
            ?.putString("email", email)
            ?.putString("userId", userId)
            ?.putInt("credits", credits)
            ?.apply()
    }

    fun updateCredits(c: Int) {
        credits = c
        prefs?.edit()?.putInt("credits", c)?.apply()
    }

    fun logout() {
        token = null
        refreshToken = null
        email = null
        userId = null
        credits = 0
        isLoggedIn = false
        prefs?.edit()?.clear()?.apply()
    }
}
