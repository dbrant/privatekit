package edu.mit.privatekit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * author: Dmitry Brant, 2020
 */
class LocationWriter {
    //@Nullable private OutputStreamWriter currentWriter;

    @Nullable private RandomAccessFile currentFile;

    @NonNull private String currentFileName = "";
    private int totalPointsWritten;
    private SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    void addPoint(@NonNull Context context, @NonNull Location location, Date date) {
        try {
            if (currentFile == null || !currentFileName.equals(fileNameDateFormat.format(date))) {
                close();
                currentFileName = fileNameDateFormat.format(date);
                File dir = new File(context.getFilesDir() + "/location");
                dir.mkdirs();

                File f = new File(dir.getAbsolutePath() + "/" + currentFileName + ".json");
                boolean exists = f.exists();

                currentFile = new RandomAccessFile(f, "rw");

                if (!exists) {
                    currentFile.writeBytes("[");
                    totalPointsWritten = 0;
                } else {
                    totalPointsWritten = 1;
                    currentFile.seek(currentFile.length() - 1);
                    if ((char)currentFile.readByte() == ']') {
                        currentFile.seek(currentFile.length() - 1);
                    }
                }
            }

            if (totalPointsWritten > 0) {
                currentFile.writeBytes(",");
            }

            currentFile.writeBytes(String.format(Locale.ROOT, "{\"latitude\":%f,\"longitude\":%f,\"altitude\":%f,\"time\":%d,\"provider\":\"%s\"}",
                    location.getLatitude(), location.getLongitude(), location.getAltitude(), date.getTime(), location.getProvider()));

            totalPointsWritten++;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    void close() {
        if (currentFile != null) {
            try {
                currentFile.writeBytes("]");
                currentFile.close();
                currentFile = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public static void shareCurrentFile(@NonNull Activity activity) {
        File dir = new File(activity.getFilesDir() + "/location/");
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        Uri uri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", files[files.length - 1]);

        Intent intent = ShareCompat.IntentBuilder.from(activity)
                .setType("application/json")
                .setStream(uri)
                .getIntent()
                .setAction(Intent.ACTION_SEND)
                .setDataAndType(uri, "application/json")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, null);
        activity.startActivity(chooser);
    }
}
