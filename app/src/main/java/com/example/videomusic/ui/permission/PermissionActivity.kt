package com.example.videomusic.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.videomusic.databinding.ActivityPermissionBinding
import com.example.videomusic.ui.main.MainActivity
import com.example.videomusic.utils.gone
import com.example.videomusic.utils.show

class PermissionActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.buttonSetting.setOnClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        viewBinding.buttonRequest.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                viewBinding.textTitle.show()
                viewBinding.buttonSetting.show()
                viewBinding.textRequest.gone()
                viewBinding.buttonRequest.gone()
            }
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            100 -> {
                if ((grantResults.isNotEmpty()) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        viewBinding.textTitle.show()
                    }
                }
            }
        }
    }
}
