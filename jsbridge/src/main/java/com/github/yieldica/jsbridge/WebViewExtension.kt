package com.github.yieldica.jsbridge

import android.webkit.WebView

var WebView.injector: Injector
    get() {
        var injector = this.getTag(R.id.webkit_bridge_injector) as? Injector
        if (injector == null) {
            injector = Injector(this)
            this.setTag(R.id.webkit_bridge_injector, injector)
        }
        return injector
    }
    set(value) {
        this.setTag(R.id.webkit_bridge_injector, value)
    }
