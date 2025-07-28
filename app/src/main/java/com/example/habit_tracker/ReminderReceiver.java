package com.example.habit_tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;


import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "HabitReminderChannel";
    private static final String CHANNEL_NAME = "Habit Tracker Reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String habitName = intent.getStringExtra("habitName");
        if (habitName == null) {
            habitName = "Habit Reminder";
        }

        // Create channel for Android 8+
        createNotificationChannel(context);

        // Get sound URI from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);
        String soundUriString = prefs.getString("notificationSound",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
        Uri soundUri = Uri.parse(soundUriString);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.download) // Replace with custom icon
                .setContentTitle("Habit Reminder")
                .setContentText("Time to work on: " + habitName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true);

        // Notify with unique ID
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (int) System.currentTimeMillis(); // Unique ID for each notification
            notificationManager.notify(notificationId, builder.build());
        }

        // 🟡 Reschedule for next day
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1); // Next day
        calendar.set(Calendar.HOUR_OF_DAY, 8); // set to same reminder time
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Intent newIntent = new Intent(context, ReminderReceiver.class);
        newIntent.putExtra("habitName", habitName);

        PendingIntent peningIntent = PendingIntent.getBroadcast(
                context,
                habitName.hashCode(), // unique ID per habit
                newIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for your habits");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
