package com.example.habit_tracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {

    private static final String TAG = "ProgressFragment";
    private ProgressBar progressBar;
    private TextView errorText;
    private BarChart barChart;
    private PieChart historyDonutChart;

    private FirebaseFirestore db;

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
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        barChart = view.findViewById(R.id.barChart);
       historyDonutChart = view.findViewById(R.id.historyProgressChart);
        db = FirebaseFirestore.getInstance();

        loadHabitData();
        loadHistoryGraph();

        return view;
    }

    private void loadHabitData() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);

        db.collection("habits")
                .get()
                .addOnSuccessListener(this::processHabitData)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    errorText.setText("Error loading data: " + e.getMessage());
                    errorText.setVisibility(View.VISIBLE);
                });
    }

    private void processHabitData(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);

        if (snapshot.isEmpty()) {
            errorText.setText("No habits found");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
        String[] allDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        List<String> displayDays = new ArrayList<>();
        int currentDayIndex = Arrays.asList(allDays).indexOf(currentDay);
        for (int i = 0; i <= currentDayIndex; i++) {
            displayDays.add(allDays[i]);
        }

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
            List<String> selectedDays = (List<String>) doc.get("days");
            String habitName = doc.getString("name");

            if (goal == null || current == null || dateStr == null || selectedDays == null)
                continue;

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(dateStr);
                if (date == null) continue;

                String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault()).format(date);
                List<String> fullDayNames = new ArrayList<>();
                for (String shortDay : selectedDays) {
                    String fullDay = DAY_NAME_MAP.getOrDefault(shortDay, shortDay);
                    fullDayNames.add(fullDay);
                }

                if (fullDayNames.contains(dayOfWeek) && displayDays.contains(dayOfWeek)) {
                    totalHabitsMap.put(dayOfWeek, totalHabitsMap.get(dayOfWeek) + 1);
                    if (current >= goal) {
                        completedHabitsMap.put(dayOfWeek, completedHabitsMap.get(dayOfWeek) + 1);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        boolean hasData = false;

        for (int i = 0; i < displayDays.size(); i++) {
            String day = displayDays.get(i);
            int total = totalHabitsMap.get(day);
            int completed = completedHabitsMap.get(day);
            float percentage = total > 0 ? (completed * 100f) / total : 0f;
            entries.add(new BarEntry(i, percentage, day));
            if (total > 0) hasData = true;
        }

        if (!hasData) {
            errorText.setText("No selected habits found for this week");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        showBarChart(entries, displayDays);
    }

    private void showBarChart(List<BarEntry> entries, List<String> labels) {
        BarDataSet dataSet = new BarDataSet(entries, "Weekly Completion %");
        dataSet.setColor(ColorTemplate.rgb("#4CAF50"));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.25f);

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
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setText("Habit Completion This Week");
        barChart.setFitBars(true);
        barChart.setVisibility(View.VISIBLE);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void loadHistoryGraph() {
        historyDonutChart.setVisibility(View.GONE);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        List<String> last7Days = new ArrayList<>();
        Map<String, Float> progressMap = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_YEAR, -i);
            String date = sdf.format(calendar.getTime());
            last7Days.add(date);
            progressMap.put(date, 0f);
        }

        db.collection("habits")
                .get()
                .addOnSuccessListener(habitSnapshots -> {
                    if (habitSnapshots.isEmpty()) return;

                    int habitCount = habitSnapshots.size();
                    int[] fetchCount = {0};
                    int totalFetch = habitCount * last7Days.size();

                    for (DocumentSnapshot habitDoc : habitSnapshots) {
                        String habitId = habitDoc.getId();

                        for (String date : last7Days) {
                            db.collection("habits")
                                    .document(habitId)
                                    .collection("history")
                                    .document(date)
                                    .get()
                                    .addOnSuccessListener(historyDoc -> {
                                        if (historyDoc.exists()) {
                                            Long count = historyDoc.getLong("count");
                                            Long goal = historyDoc.getLong("goal");
                                            if (count != null && goal != null && goal > 0) {
                                                float percent = (count * 100f) / goal;
                                                progressMap.put(date, progressMap.get(date) + percent);
                                            }
                                        }

                                        fetchCount[0]++;
                                        if (fetchCount[0] == totalFetch) {
                                            showHistoryGraph(progressMap, habitCount);
                                        }
                                    });
                        }
                    }
                });
    }



    private void showHistoryGraph(Map<String, Float> progressMap, int habitCount) {
        float totalProgress = 0;
        for (float dailyProgress : progressMap.values()) {
            totalProgress += dailyProgress;
        }

        float averageProgress = habitCount > 0 ? totalProgress / (habitCount * progressMap.size()) : 0;
        float remaining = 100f - averageProgress;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(averageProgress, "Done"));
        entries.add(new PieEntry(remaining, "Remaining"));

        PieDataSet dataSet = new PieDataSet(entries, "Avg Progress (7 Days)");
        dataSet.setColors(Color.rgb(76, 175, 80), Color.LTGRAY);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData pieData = new PieData(dataSet);

        historyDonutChart.setData(pieData);
        historyDonutChart.setDrawHoleEnabled(true);
        historyDonutChart.setHoleRadius(50f); // controls hole size
        historyDonutChart.setCenterText(String.format(Locale.getDefault(), "%.0f%%", averageProgress));
        historyDonutChart.setCenterTextSize(16f);
        historyDonutChart.getDescription().setEnabled(false);
        historyDonutChart.setUsePercentValues(true);
        historyDonutChart.setDrawEntryLabels(false);
        historyDonutChart.setVisibility(View.VISIBLE);
        historyDonutChart.animateY(1000);
        historyDonutChart.invalidate();
    }
}
