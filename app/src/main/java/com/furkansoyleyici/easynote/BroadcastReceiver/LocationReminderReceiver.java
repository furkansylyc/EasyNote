package com.furkansoyleyici.easynote.BroadcastReceiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.furkansoyleyici.easynote.Firebase.FirestoreManager;
import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.R;

import java.util.List;

public class LocationReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "location_reminder_channel";
    private static final int NOTIFICATION_ID = 200;

    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        
        if (!notificationsEnabled) {
            return;
        }
        
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String locationName = intent.getStringExtra("locationName");
        String firestoreId = intent.getStringExtra("firestoreId");
        
        createNotificationChannel(context);
        showNotification(context, title, message, locationName);
        
        if (firestoreId != null && !firestoreId.isEmpty()) {
            updateNoteAfterNotification(context, firestoreId);
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.location_reminder_channel_name);
            String description = context.getString(R.string.location_reminder_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, String title, String message, String locationName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String notificationTitle = context.getString(R.string.location_reminder_title, title);
        String notificationText = context.getString(R.string.location_reminder_text, message, locationName);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.en)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void updateNoteAfterNotification(Context context, String firestoreId) {
        FirestoreManager firestoreManager = new FirestoreManager();
        com.furkansoyleyici.easynote.Model.Notes updateNote = new com.furkansoyleyici.easynote.Model.Notes();
        updateNote.hasLocationReminder = false;
        firestoreManager.updateNote(firestoreId, updateNote, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                android.util.Log.d("LocationReminderReceiver", "Konum hatırlatıcısı Firestore'da kapatıldı - FirestoreID: " + firestoreId);
            }
            @Override
            public void onFailure(String error) {
                android.util.Log.e("LocationReminderReceiver", "Konum hatırlatıcısı güncellenemedi (Firestore): " + error);
            }
        });
    }
} 