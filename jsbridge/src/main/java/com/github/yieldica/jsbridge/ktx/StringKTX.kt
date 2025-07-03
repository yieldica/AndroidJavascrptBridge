package com.github.yieldica.jsbridge.ktx

import java.security.MessageDigest

internal val String.md5: String
    get() {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
