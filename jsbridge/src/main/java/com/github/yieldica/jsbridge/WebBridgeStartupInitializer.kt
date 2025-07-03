package com.github.yieldica.jsbridge

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.github.yieldica.jsbridge.script.UserScriptManager
import com.github.yieldica.jsbridge.utils.ApplicationHolder

@Suppress("unused")
class WebBridgeStartupInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        ApplicationHolder.application = context.applicationContext as Application
        UserScriptManager.initializer(context)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}