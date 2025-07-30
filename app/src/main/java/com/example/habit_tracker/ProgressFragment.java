package com.example.habit_tracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {

    private static final String TAG = "ProgressFragment";
    private ProgressBar progressBar;
    private TextView errorText;
    private BarChart barChart;
    private FirebaseFirestore db;
    private List<String> last7Days;
    private List<Integer> colors;

    // Map abbreviated day names to full names
    private static final Map<String, String> DAY_NAME_MAP = new HashMap<>();
    static {
        DAY_NAME_MAP.put("Mon", "Monday");
        DAY_NAME_MAP.put("Tue", "Tuesday");
        DAY_NAME_MAP.put("Wed", "Wednesday");
        DAY_NAME_MAP.put("Thu", "Thursday");
        DAY_NAME_MAP.put("Fri", "Friday");
        DAY_NAME_MAP.put("Sat", "Saturday");
        DAY_NAME_MAP.put("Sun", "Sunday");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Initializing view");
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        barChart = view.findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        if (barChart == null) {
            Log.e(TAG, "BarChart view is null. Check fragment_progress.xml");
            errorText.setText("Error: BarChart not found in layout");
            errorText.setVisibility(View.VISIBLE);
            return view;
        }

        loadHabitData();
        return view;
    }

    private void loadHabitData() {
        Log.d(TAG, "loadHabitData: Fetching habits from Firestore");
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);

        db.collection("habits")
                .get()
                .addOnSuccessListener(this::processHabitData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading habits: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    errorText.setText("Error loading data: " + e.getMessage());
                    errorText.setVisibility(View.VISIBLE);
                });
    }

    private void processHabitData(QuerySnapshot snapshot) {
        Log.d(TAG, "processHabitData: Processing " + snapshot.size() + " documents");
        progressBar.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            Log.w(TAG, "No habits found in Firestore");
            errorText.setText("No habits found");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        // Get current day of the week
        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
        Log.d(TAG, "Current day: " + currentDay);

        // Define ordered days of the week
        String[] allDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        List<String> displayDays = new ArrayList<>();
        int currentDayIndex = Arrays.asList(allDays).indexOf(currentDay);

        // Include only days up to the current day
        for (int i = 0; i <= currentDayIndex; i++) {
            displayDays.add(allDays[i]);
        }

        // Maps for total and completed habits per day
        Map<String, Integer> totalHabitsMap = new LinkedHashMap<>();
        Map<String, Integer> completedHabitsMap = new LinkedHashMap<>();
        for (String day : displayDays) {
            totalHabitsMap.put(day, 0);
            completedHabitsMap.put(day, 0);
        }

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long goal = doc.getLong("goal");
            Long current = doc.getLong("currentCount");
            String dateStr = doc.getString("lastUpdatedDate");
            List<String> selectedDays = (List<String>) doc.get("days"); // Match HomeFragment's field
            String habitName = doc.getString("name");

            if (goal == null || current == null || dateStr == null || selectedDays == null) {
                Log.w(TAG, "Invalid document data: " + doc.getId() + ", goal=" + goal + ", current=" + current +
                        ", dateStr=" + dateStr + ", days=" + selectedDays);
                continue;
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(dateStr);
                if (date == null) {
                    Log.w(TAG, "Failed to parse date: " + dateStr);
                    continue;
                }

                String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
                // Convert selectedDays' abbreviated names to full names
                List<String> fullDayNames = new ArrayList<>();
                for (String shortDay : selectedDays) {
                    String fullDay = DAY_NAME_MAP.getOrDefault(shortDay, shortDay);
                    fullDayNames.add(fullDay);
                }

                if (fullDayNames.contains(dayOfWeek) && displayDays.contains(dayOfWeek)) {
                    Log.d(TAG, "Habit '" + habitName + "' matches day: " + dayOfWeek);
                    totalHabitsMap.put(dayOfWeek, totalHabitsMap.getOrDefault(dayOfWeek, 0) + 1);
                    if (current >= goal) {
                        completedHabitsMap.put(dayOfWeek, completedHabitsMap.getOrDefault(dayOfWeek, 0) + 1);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing document " + doc.getId() + ": " + e.getMessage());
            }
        }

        // Calculate completion percentages
        List<BarEntry> entries = new ArrayList<>();
        boolean hasData = false;

        for (int i = 0; i < displayDays.size(); i++) {
            String day = displayDays.get(i);
            int total = totalHabitsMap.get(day);
            int completed = completedHabitsMap.get(day);
            float percentage = total > 0 ? (completed * 100f) / total : 0f;
            entries.add(new BarEntry(i, percentage, day));
            Log.d(TAG, day + ": " + completed + "/" + total + " = " + percentage + "%");
            if (total > 0) hasData = true;
        }

        if (!hasData) {
            Log.w(TAG, "No selected habits found for days up to " + currentDay);
            errorText.setText("No selected habits found for days up to " + currentDay);
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        showBarChart(entries, displayDays);
    }

    private void showBarChart(List<BarEntry> entries, List<String> labels) {
        Log.d(TAG, "showBarChart: Displaying chart with " + entries.size() + " entries");
        BarDataSet dataSet = new BarDataSet(entries, "Weekly Progress");
        dataSet.setColor(ColorTemplate.rgb("#4CAF50")); // Green for bars
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.12f); // Narrow bars for dynamic number of days

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return index >= 0 && index < labels.size() ? labels.get(index).substring(0, 3) : "";
            }
        });

        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setText("Habit Completion Up to Today");
        barChart.setFitBars(true);
        barChart.setVisibility(View.VISIBLE);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}