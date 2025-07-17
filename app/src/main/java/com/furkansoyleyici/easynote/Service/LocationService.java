package com.furkansoyleyici.easynote.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Model.Notes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 100;
    private FirestoreManager firestoreManager;
    private ExecutorService executorService;
    private static final float LOCATION_RADIUS = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firestoreManager = new FirestoreManager();
        executorService = Executors.newSingleThreadExecutor();
        createLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();
        return START_STICKY;
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location currentLocation = locationResult.getLastLocation();
                    if (currentLocation != null) {
                        checkLocationReminders(currentLocation);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setIntervalMillis(30000)
                .setMinUpdateIntervalMillis(10000)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
        }
    }

    private void checkLocationReminders(Location currentLocation) {
        executorService.execute(() -> {
            SharedPreferences prefs = getSharedPreferences("TriggeredNotes", MODE_PRIVATE);
            Set<String> triggeredNotes = new HashSet<>(prefs.getStringSet("triggered", new HashSet<>()));

            firestoreManager.getAllNotes(new FirestoreManager.NotesCallback() {
                @Override
                public void onNotesLoaded(List<Notes> allNotes) {
                    for (Notes note : allNotes) {
                        if (note.hasLocationReminder && note.latitude != 0 && note.longitude != 0) {
                            Location noteLocation = new Location("note");
                            noteLocation.setLatitude(note.latitude);
                            noteLocation.setLongitude(note.longitude);

                            float distance = currentLocation.distanceTo(noteLocation);

                            if (distance <= LOCATION_RADIUS && !triggeredNotes.contains(note.firestoreId)) {
                                triggerLocationReminder(note);

                                // Firestore'da hasLocationReminder'ı false yap
                                Notes updateNote = new Notes();
                                updateNote.hasLocationReminder = false;
                                firestoreManager.updateNote(note.firestoreId, updateNote, new FirestoreManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {}
                                    @Override
                                    public void onFailure(String error) {}
                                });

                                triggeredNotes.add(note.firestoreId);
                                prefs.edit().putStringSet("triggered", triggeredNotes).apply();
                            }
                        }
                    }
                }
                @Override
                public void onError(String error) {}
            });
        });
    }


    private void triggerLocationReminder(Notes note) {
        Intent intent = new Intent(this, com.furkansoyleyici.easynote.BroadcastReceiver.LocationReminderReceiver.class);
        intent.putExtra("title", note.notesTitle);
        intent.putExtra("message", note.notes);
        intent.putExtra("locationName", note.locationName);
        intent.putExtra("firestoreId", note.firestoreId);
        sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Konum Servisi";
            String description = "Konum tabanlı hatırlatıcılar için";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(
                    "silent_channel",
                    "Sessiz Kanal",
                    NotificationManager.IMPORTANCE_NONE
            );
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "silent_channel")
                .setSmallIcon(R.drawable.en)
                .setContentTitle(getString(R.string.location_service_active))
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setSilent(true);

        return builder.build();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 