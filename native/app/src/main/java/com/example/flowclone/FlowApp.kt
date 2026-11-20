package com.example.flowclone

import android.app.Application

class FlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
    }
}
