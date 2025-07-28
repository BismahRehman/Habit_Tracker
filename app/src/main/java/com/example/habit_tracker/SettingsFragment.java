package com.example.habit_tracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class SettingsFragment extends Fragment {

    private static final int REQUEST_CODE_NOTIFICATION_SOUND = 100;
    private Switch switchReminders, switchDarkMode;
    private TextView txtReminderTime, txtAccountInfo;
    private Button btnLogout, btnClearHistory, btnSelectSound;
    private Spinner languageSpinner;
    private Calendar reminderCalendar = Calendar.getInstance();
    private SharedPreferences prefs;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchReminders = view.findViewById(R.id.switchReminders);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        txtReminderTime = view.findViewById(R.id.txtReminderTime);
        txtAccountInfo = view.findViewById(R.id.txtAccountInfo);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);
        btnSelectSound = view.findViewById(R.id.btnSelectSound);
        languageSpinner = view.findViewById(R.id.languageSpinner);

        prefs = requireContext().getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();

        initializePreferences();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        txtAccountInfo.setText(user != null ? "Email: " + user.getEmail() : "Email: Not Logged In");

        switchReminders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            txtReminderTime.setEnabled(isChecked);
            prefs.edit().putBoolean("remindersEnabled", isChecked).apply();
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !requireContext().getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
                    Toast.makeText(getContext(), "Please allow exact alarm permissions in system settings", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    switchReminders.setChecked(false);
                    return;
                }
                scheduleNotification();
            } else {
                cancelNotification();
            }
        });

        txtReminderTime.setOnClickListener(v -> showTimePicker());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            requireActivity().recreate();
        });

        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, Arrays.asList("English", "Urdu", "French"));
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        languageSpinner.setSelection(getLanguagePosition(prefs.getString("language", "English")));

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLang = parent.getItemAtPosition(position).toString();
                if (!selectedLang.equals(prefs.getString("language", "English"))) {
                    prefs.edit().putString("language", selectedLang).apply();
                    setLocale(selectedLang);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSelectSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(prefs.getString("notificationSound",
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
            startActivityForResult(intent, REQUEST_CODE_NOTIFICATION_SOUND);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Navigate to LoginActivity after logout
            Intent intent = new Intent(getContext(), LoginActivity.class); // change if your login screen has a different name
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // clear activity stack
            startActivity(intent);
            requireActivity().finish(); // finish current activity
        });

        btnClearHistory.setOnClickListener(v -> {
            db.collection("habits").get().addOnSuccessListener(snap -> {
                for (var doc : snap.getDocuments()) {
                    doc.getReference().collection("history").get().addOnSuccessListener(historySnap -> {
                        for (var h : historySnap.getDocuments()) {
                            h.getReference().delete();
                        }
                    });
                }
                Toast.makeText(getContext(), "Habit history cleared", Toast.LENGTH_SHORT).show();
            });
        });

        return view;
    }

    private void initializePreferences() {
        boolean remindersEnabled = prefs.getBoolean("remindersEnabled", false);
        switchReminders.setChecked(remindersEnabled);
        txtReminderTime.setEnabled(remindersEnabled);

        String savedTime = prefs.getString("reminderTime", "10:00");
        txtReminderTime.setText("Reminder Time: " + savedTime);
        String[] timeParts = savedTime.split(":");
        try {
            reminderCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            reminderCalendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        } catch (NumberFormatException e) {
            reminderCalendar.set(Calendar.HOUR_OF_DAY, 10);
            reminderCalendar.set(Calendar.MINUTE, 0);
        }

        if (remindersEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                requireContext().getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
            scheduleNotification();
        }
    }

    private void showTimePicker() {
        int hour = reminderCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = reminderCalendar.get(Calendar.MINUTE);
        new TimePickerDialog(getContext(), (view, hourOfDay, minute1) -> {
            reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            reminderCalendar.set(Calendar.MINUTE, minute1);
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
            txtReminderTime.setText("Reminder Time: " + time);
            prefs.edit().putString("reminderTime", time).apply();
            if (switchReminders.isChecked()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !requireContext().getSystemService(AlarmManager.class).canScheduleExactAlarms()) {
                    Toast.makeText(getContext(), "Please allow exact alarm permissions in system settings", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    return;
                }
                scheduleNotification();
            }
        }, hour, minute, false).show();
    }

    private void scheduleNotification() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("showYesterday", true); // Signal to show yesterday's habits
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminderCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, reminderCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            Toast.makeText(getContext(), "Reminder set for " + txtReminderTime.getText(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Failed to set reminder: Please allow exact alarm permissions in system settings", Toast.LENGTH_LONG).show();
            Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            settingsIntent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(settingsIntent);
            switchReminders.setChecked(false);
            prefs.edit().putBoolean("remindersEnabled", false).apply();
        }
    }

    private void cancelNotification() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(getContext(), "Reminder cancelled", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Failed to cancel reminder: Please check permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void setLocale(String language) {
        String langCode = language.equals("Urdu") ? "ur" : language.equals("French") ? "fr" : "en";
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        requireContext().getResources().updateConfiguration(config,
                requireContext().getResources().getDisplayMetrics());
        Toast.makeText(getContext(), "Language changed to " + language, Toast.LENGTH_SHORT).show();
        requireActivity().recreate();
    }

    private int getLanguagePosition(String language) {
        switch (language) {
            case "Urdu": return 1;
            case "French": return 2;
            default: return 0;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NOTIFICATION_SOUND && resultCode == Activity.RESULT_OK && data != null) {
            Uri soundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (soundUri != null) {
                prefs.edit().putString("notificationSound", soundUri.toString()).apply();
                Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), soundUri);
                ringtone.play();
                new android.os.Handler().postDelayed(ringtone::stop, 2000); // Stop after 2 seconds
                Toast.makeText(getContext(), "Notification sound selected", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("notificationSound",
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()).apply();
                Toast.makeText(getContext(), "Default notification sound selected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}