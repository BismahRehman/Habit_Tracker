package com.example.habit_tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class FabFragment extends Fragment {

    private EditText etHabitName, etHabitDesc, etGoal;
    private CheckBox sun, mon, tue, wed, thu, fri, sat, reminderCheck;
    private TextView timeText;
    private Button btnSave;
    private FirebaseFirestore db;
    private String selectedTime = "";
    private String docIdToEdit = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fab, container, false);

        etHabitName = view.findViewById(R.id.etHabitName);
        etHabitDesc = view.findViewById(R.id.etHabitDesc);
        etGoal = view.findViewById(R.id.etGoal);

        sun = view.findViewById(R.id.sun);
        mon = view.findViewById(R.id.mon);
        tue = view.findViewById(R.id.tue);
        wed = view.findViewById(R.id.wed);
        thu = view.findViewById(R.id.thu);
        fri = view.findViewById(R.id.fri);
        sat = view.findViewById(R.id.sat);
        reminderCheck = view.findViewById(R.id.reminderCheck);
        timeText = view.findViewById(R.id.timeDisplay);
        btnSave = view.findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();


        reminderCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) showTimePicker();
            else {
                timeText.setText("No time selected");
                selectedTime = "";
            }

        });

        btnSave.setOnClickListener(v -> saveHabit());

        // ✅ Move argument check inside onCreateView
        if (getArguments() != null) {
            docIdToEdit = getArguments().getString("docId");
            etHabitName.setText(getArguments().getString("name"));
            etHabitDesc.setText(getArguments().getString("description"));
            etGoal.setText(String.valueOf(getArguments().getLong("goal")));
            // ✅ Restore reminder
            boolean reminder = getArguments().getBoolean("reminder", false);
            reminderCheck.setChecked(reminder);
            selectedTime = getArguments().getString("reminderTime", "");
            if (!selectedTime.isEmpty()) {
                timeText.setText("Reminder Time: " + selectedTime);
            }

            // ✅ Restore selected days
            ArrayList<String> selectedDays = getArguments().getStringArrayList("days");
            if (selectedDays != null) {
                sun.setChecked(selectedDays.contains("Sun"));
                mon.setChecked(selectedDays.contains("Mon"));
                tue.setChecked(selectedDays.contains("Tue"));
                wed.setChecked(selectedDays.contains("Wed"));
                thu.setChecked(selectedDays.contains("Thu"));
                fri.setChecked(selectedDays.contains("Fri"));
                sat.setChecked(selectedDays.contains("Sat"));
            }
        }

        return view;
    }


    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute1) -> {
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
            timeText.setText("Reminder Time: " + selectedTime);
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void saveHabit() {
        String name = etHabitName.getText().toString().trim();
        String desc = etHabitDesc.getText().toString().trim();
        String goalStr = etGoal.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(desc) || TextUtils.isEmpty(goalStr)){
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int goal = Integer.parseInt(goalStr);
        boolean reminder = reminderCheck.isChecked();

        List<String> days = new ArrayList<>();
        if (sun.isChecked()) days.add("Sun");
        if (mon.isChecked()) days.add("Mon");
        if (tue.isChecked()) days.add("Tue");
        if (wed.isChecked()) days.add("Wed");
        if (thu.isChecked()) days.add("Thu");
        if (fri.isChecked()) days.add("Fri");
        if (sat.isChecked()) days.add("Sat");

        Map<String, Object> habit = new HashMap<>();
        habit.put("name", name); // ✅ Correct key
        habit.put("description", desc);
        habit.put("goal", goal);
        habit.put("currentCount", 0); // ✅ matches HabitModel
        habit.put("streak", 0);
        habit.put("days", days);
        habit.put("reminder", reminder);
        habit.put("reminderTime", selectedTime);

        // save in global habits collection
        if (docIdToEdit != null) {
            db.collection("habits").document(docIdToEdit)
                    .set(habit)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(getContext(), "Habit updated!", Toast.LENGTH_SHORT).show();
                        requireActivity().onBackPressed();
                    });
        } else {
            db.collection("habits").add(habit)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(getContext(), "Habit saved!", Toast.LENGTH_SHORT).show();

                    });
        }


        if (reminder && !TextUtils.isEmpty(selectedTime)) {
            String[] parts = selectedTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            Intent intent = new Intent(getContext(), ReminderReceiver.class);
            intent.putExtra("habitName", name);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                Log.d("Reminder", "Alarm set for: " + calendar.getTime());
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
            }
        }

    }


}
