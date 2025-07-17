package com.furkansoyleyici.easynote.BroadcastReceiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Model.Notes;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderReceiver extends BroadcastReceiver {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        
        if (!notificationsEnabled) {
            return;
        }
        
        String noteTitle = intent.getStringExtra("title");
        String noteMessage = intent.getStringExtra("message");
        String firestoreId = intent.getStringExtra("firestoreId");

        showNotification(context, noteTitle, noteMessage);
        
        if (firestoreId != null && !firestoreId.isEmpty()) {
            clearAlarmsAfterNotification(context, firestoreId);
        }
    }

    private void showNotification(Context context, String noteTitle, String noteMessage) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = "note_reminder_channel";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Not Hatırlatma Kanalı",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.en)
                .setContentTitle(noteTitle != null ? noteTitle : "Notunuzu Hatırlayın!")
                .setContentText(noteMessage != null ? noteMessage : "Bir notunuz var!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void clearAlarmsAfterNotification(Context context, String firestoreId) {
        FirestoreManager firestoreManager = new FirestoreManager();
        Notes updateNote = new Notes();
        updateNote.alarmTimes = "[]";
        updateNote.hasAlarm = false;
        firestoreManager.updateNote(firestoreId, updateNote, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                android.util.Log.d("ReminderReceiver", "Alarmlar  temizlendi  " );
            }
            @Override
            public void onFailure(String error) {
                android.util.Log.e("ReminderReceiver", "Alarm temizleme hatası (Firestore): " + error);
            }
        });
    }
}
