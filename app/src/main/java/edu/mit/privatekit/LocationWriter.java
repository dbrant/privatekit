package edu.mit.privatekit;

import android.content.Context;
import android.location.Location;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * author: Dmitry Brant, 2020
 */
class LocationWriter {
    @Nullable private OutputStreamWriter currentWriter;
    @NonNull private String currentFileName = "";
    private int totalPointsWritten;
    private SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    void addPoint(@NonNull Context context, @NonNull Location location, Date date) {
        try {
            if (currentWriter == null || !currentFileName.equals(fileNameDateFormat.format(date))) {
                close();
                currentFileName = fileNameDateFormat.format(date);
                File f = new File(context.getFilesDir() + "/" + currentFileName + ".json");
                currentWriter = new OutputStreamWriter(new FileOutputStream(f, true));
                currentWriter.write("[");
                totalPointsWritten = 0;
            }

            if (totalPointsWritten > 0) {
                currentWriter.write(",");
            }

            currentWriter.write(String.format(Locale.ROOT, "{\"latitude\":%f,\"longitude\":%f,\"time\":%d}",
                    location.getLatitude(), location.getLongitude(), date.getTime()));

            totalPointsWritten++;

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    void close() {
        if (currentWriter != null) {
            try {
                currentWriter.write("]");
                currentWriter.flush();
                currentWriter.close();
                currentWriter = null;
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
