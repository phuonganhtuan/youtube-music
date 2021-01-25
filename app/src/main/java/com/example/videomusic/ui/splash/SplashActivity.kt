package com.example.videomusic.ui.splash

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.videomusic.databinding.ActivitySplashBinding
import com.example.videomusic.ui.main.MainActivity
import com.example.videomusic.ui.permission.PermissionActivity
import com.example.videomusic.utils.show

class SplashActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        animateOpenMainScreen()
    }

    private fun openMainScreen() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, PermissionActivity::class.java))
        }
        finish()
    }

    private fun animateOpenMainScreen() {
        val handlerAnimation = Handler(Looper.getMainLooper())
        handlerAnimation.postDelayed({
            viewBinding.textAppName.show()
        }, 700)
        Handler(Looper.getMainLooper()).postDelayed({
            openMainScreen()
        }, 1200)
    }
}
