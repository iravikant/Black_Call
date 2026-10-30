package com.blackcall

import android.app.Application
import android.content.Context
import com.blackcall.di.AppContainer

class BlackCallApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannel()
    }
}

val Context.container: AppContainer
    get() = (applicationContext as BlackCallApp).container
