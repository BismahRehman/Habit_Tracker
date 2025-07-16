package com.example.habit_tracker;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Intent to open MainActivity when notification is tapped
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.putExtra("showYesterday", intent.getBooleanExtra("showYesterday", false));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get saved notification sound
        SharedPreferences prefs = context.getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);
        String soundUriString = prefs.getString("notificationSound",
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

        Uri soundUri = Uri.parse(soundUriString);

        // Build and send notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "habit_tracker_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Update to your app's notification icon
                .setContentTitle("Habit Tracker")
                .setContentText("Time to check your habits!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(100, builder.build());
    }
}
