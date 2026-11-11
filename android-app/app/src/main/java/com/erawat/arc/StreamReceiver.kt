package com.erawat.arc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StreamReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == StreamService.ACTION_STOP) {
            StreamService.stop(context)
        }
    }
}
