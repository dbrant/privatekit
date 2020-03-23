package edu.mit.privatekit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*

/**
 * author: Dmitry Brant, 2020
 */
internal class LocationWriter {
    private var currentFile: RandomAccessFile? = null
    private var currentFileName = ""
    private var totalPointsWritten = 0
    private val fileNameDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    fun addPoint(context: Context, location: Location, date: Date) {
        try {
            if (currentFile == null || currentFileName != fileNameDateFormat.format(date)) {
                close()
                currentFileName = fileNameDateFormat.format(date)
                val dir = File(context.filesDir.toString() + "/location")
                dir.mkdirs()
                val f = File(dir.absolutePath + "/" + currentFileName + ".json")
                val exists = f.exists()
                currentFile = RandomAccessFile(f, "rw")
                if (!exists) {
                    currentFile!!.writeBytes("[")
                    totalPointsWritten = 0
                } else {
                    totalPointsWritten = 1
                    currentFile!!.seek(currentFile!!.length() - 1)
                    if (currentFile!!.readByte().toChar() == ']') {
                        currentFile!!.seek(currentFile!!.length() - 1)
                    }
                }
            }
            if (totalPointsWritten > 0) {
                currentFile!!.writeBytes(",")
            }
            currentFile!!.writeBytes(
                String.format(
                    Locale.ROOT,
                    "{\"latitude\":%f,\"longitude\":%f,\"altitude\":%f,\"time\":%d,\"provider\":\"%s\"}",
                    location.latitude,
                    location.longitude,
                    location.altitude,
                    date.time,
                    location.provider
                )
            )
            totalPointsWritten++
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun close() {
        if (currentFile != null) {
            try {
                currentFile!!.writeBytes("]")
                currentFile!!.close()
                currentFile = null
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        fun shareCurrentFile(activity: Activity) {
            val dir = File(activity.filesDir.toString() + "/location/")
            val files = dir.listFiles()
            if (files == null || files.isEmpty()) {
                return
            }
            val uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", files[files.size - 1])
            val intent = ShareCompat.IntentBuilder.from(activity)
                .setType("application/json")
                .setStream(uri)
                .intent
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(uri, "application/json")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, null)
            activity.startActivity(chooser)
        }
    }
}