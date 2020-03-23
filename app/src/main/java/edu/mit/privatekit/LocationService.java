package edu.mit.privatekit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.util.Date;

/**
 * author: Dmitry Brant, 2020
 */
public class LocationService extends Service {
    private static final String TAG = "LocationService";
    public static final String PREFS_NAME = "LocationServicePrefs";
    public static final String PREF_IS_STARTED = "isStarted";

    private static final int NOTIFICATION_ID = 1001;
    private static final String NOTIFICATION_CHANNEL_ID = "PrivateKitChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Notification channel for PrivateKit";

    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0f;

    public static boolean IS_RUNNING = false;

    private LocationManager locationManager = null;
    private LocationListener locationListenerFine = new LocationListener();
    private LocationListener locationListenerCoarse = new LocationListener();
    private LocationWriter locationWriter = new LocationWriter();

    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, LocationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(@NonNull Context context) {
        Intent intent = new Intent(context, LocationService.class);
        context.stopService(intent);
    }

    private class LocationListener implements android.location.LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged: " + location);
            try {
                locationWriter.addPoint(LocationService.this, location, new Date());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(LocationService.this, getString(R.string.error_file_write, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged: " + provider);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }

        startForeground(NOTIFICATION_ID, createNotification());

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL, LOCATION_DISTANCE, locationListenerFine);
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(LocationService.this, getString(R.string.error_file_write, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    LOCATION_INTERVAL * 2, LOCATION_DISTANCE, locationListenerCoarse);
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(LocationService.this, getString(R.string.error_file_write, e.getLocalizedMessage()), Toast.LENGTH_LONG).show();
        }

        persistStartedState(true);
    }

    @Override
    public void onDestroy() {
        persistStartedState(false);
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(locationListenerFine);
            } catch (Exception e) {
                // ignore
            }
            try {
                locationManager.removeUpdates(locationListenerCoarse);
            } catch (Exception e) {
                // ignore
            }
        }
        if (locationWriter != null) {
            locationWriter.close();
        }
    }

    private Notification createNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_my_location_black_24dp)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setAutoCancel(false);
        return builder.build();
    }

    private void persistStartedState(boolean started) {
        IS_RUNNING = started;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_IS_STARTED, started);
        editor.apply();
    }
}
