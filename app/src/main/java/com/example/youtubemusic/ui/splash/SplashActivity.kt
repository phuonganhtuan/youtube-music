package com.example.youtubemusic.ui.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.youtubemusic.databinding.ActivitySplashBinding
import com.example.youtubemusic.ui.main.MainActivity
import com.example.youtubemusic.utils.show

class SplashActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        animateOpenMainScreen()
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
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
