package com.cloudacr.app.utils

import android.media.AudioManager
import android.content.Context

object AudioUtils {
    fun isCallActive(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java)
        return am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    fun isSpeakerOn(context: Context): Boolean {
        val am = context.getSystemService(AudioManager::class.java)
        return am.isSpeakerphoneOn
    }
}
