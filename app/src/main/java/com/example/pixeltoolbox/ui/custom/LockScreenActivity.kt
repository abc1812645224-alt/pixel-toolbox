package com.example.pixeltoolbox.ui.custom

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.example.pixeltoolbox.shizuku.ShizukuUtils

class LockScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ShizukuUtils.hasShizukuPermission()) {
            ShizukuUtils.executeCommand("input keyevent 26")
        } else {
            Toast.makeText(this, "请先激活 Shizuku 权限", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
