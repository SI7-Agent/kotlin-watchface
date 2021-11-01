package com.si7agent.digital_neon

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.ContextCompat

class RequestActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.request_permissions_activity)

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.BODY_SENSORS), 0)
        }

        finish()
    }
}