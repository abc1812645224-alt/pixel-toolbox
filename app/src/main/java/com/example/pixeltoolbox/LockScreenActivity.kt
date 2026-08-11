package com.example.pixeltoolbox

import android.app.Activity
import android.os.Bundle
import com.example.pixeltoolbox.shizuku.ShizukuUtils

class LockScreenActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Execute the power button event to lock the screen via Shizuku
        ShizukuUtils.executeCommandOrNull("input keyevent 26")
        
        // Finish the activity immediately so it remains completely transparent/invisible
        finish()
    }
}
