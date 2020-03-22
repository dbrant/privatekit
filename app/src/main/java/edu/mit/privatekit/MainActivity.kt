package edu.mit.privatekit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.activity_main.*

/**
 * author: Dmitry Brant, 2020
 */
class MainActivity : AppCompatActivity() {
    private var serviceIntent: Intent? = null
    private var serviceCheckRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceIntent = Intent(this, LocationService::class.java)

        btnStartStop.setOnClickListener {
            if (!LocationService.IS_RUNNING) {
                checkPermissionsThenStart()
            } else {
                stopService(serviceIntent)
            }
        }

        btnExport.setOnClickListener {
            LocationWriter.shareCurrentFile(this)
        }

        serviceCheckRunnable = Runnable {
            if (LocationService.IS_RUNNING) {
                btnStartStop.text = getString(R.string.stop_logging)
                ViewCompat.setBackgroundTintList(btnStartStop, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_stop)))
            } else {
                btnStartStop.text = getString(R.string.start_logging)
                ViewCompat.setBackgroundTintList(btnStartStop, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_start)))
            }
            btnStartStop.postDelayed(serviceCheckRunnable, 1000)
        }
        btnStartStop.post(serviceCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        btnStartStop.removeCallbacks(serviceCheckRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                var allGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                        break
                    }
                }

                if (allGranted) {
                    checkPermissionsThenStart()
                } else {
                    // TODO: permission denied
                }
                return
            }
        }
    }

    private fun checkPermissionsThenStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            // Permissions not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // TODO: Show an explanation to the user
            } else {
                // request the permission
                var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
            }
        } else {
            // Permissions granted
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    companion object Constants {
        const val PERMISSIONS_REQUEST_CODE = 100
    }
}
