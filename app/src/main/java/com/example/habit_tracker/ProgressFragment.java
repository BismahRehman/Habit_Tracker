package com.example.habit_tracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {

    private Spinner chartSelector;
    private LineChart lineChart;
    private PieChart pieChart;
    private BarChart barChart;
    private ProgressBar progressBar;
    private TextView errorText;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        // UI references
        chartSelector = view.findViewById(R.id.chartSelector);
        lineChart = view.findViewById(R.id.lineChart);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);

        db = FirebaseFirestore.getInstance();

        // Chart type selector
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Line", "Pie", "Bar"});
        chartSelector.setAdapter(adapter);

        chartSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadHabitData();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        progressBar.setVisibility(View.VISIBLE);
        hideAllCharts();

        return view;
    }

    private void loadHabitData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("habits").get().addOnSuccessListener(querySnapshot -> {
            progressBar.setVisibility(View.GONE);
            processHabitData(querySnapshot.getDocuments());
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            errorText.setText("Failed to load habits");
            errorText.setVisibility(View.VISIBLE);
            hideAllCharts();
        });
    }

    private void processHabitData(List<DocumentSnapshot> documents) {
        Map<Integer, List<Float>> dayToPercentList = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (DocumentSnapshot doc : documents) {
            Long goal = doc.getLong("goal");
            Long current = doc.getLong("currentCount");
            String dateStr = doc.getString("lastUpdatedDate");
            List<String> days = (List<String>) doc.get("days");

            if (goal != null && current != null && dateStr != null) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sdf.parse(dateStr));
                    int day = cal.get(Calendar.DAY_OF_WEEK);
                    String dayStr = getDayString(day);
                    float percent = Math.min((current * 100f) / goal, 100f);

                    if (days != null && days.contains(dayStr)) {
                        dayToPercentList.computeIfAbsent(day, k -> new ArrayList<>()).add(percent);
                    }
                } catch (Exception ignored) {}
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            List<Float> percents = dayToPercentList.getOrDefault(day, new ArrayList<>());
            float avg = percents.isEmpty() ? 0f : (float) percents.stream().mapToDouble(f -> f).average().orElse(0);
            entries.add(new Entry(day, avg));
        }

        if (entries.stream().allMatch(e -> e.getY() == 0f)) {
            errorText.setText("No habit progress data");
            errorText.setVisibility(View.VISIBLE);
            hideAllCharts();
        } else {
            errorText.setVisibility(View.GONE);
            showSelectedChart(entries);
        }
    }

    private void showSelectedChart(List<Entry> entries) {
        String selected = chartSelector.getSelectedItem().toString();
        hideAllCharts();
        switch (selected) {
            case "Line": showLineChart(entries); break;
            case "Pie": showPieChart(entries); break;
            case "Bar": showBarChart(entries); break;
        }
    }

    private void showLineChart(List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Progress (%)");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.BLUE);
        LineData lineData = new LineData(dataSet);

        lineChart.setData(lineData);
        lineChart.getDescription().setText("Line Chart");
        setXAxisLabels(lineChart.getXAxis());
        lineChart.getAxisLeft().setAxisMaximum(100f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setVisibility(View.VISIBLE);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private void showPieChart(List<Entry> entries) {
        List<PieEntry> pieEntries = new ArrayList<>();
        for (Entry e : entries) {
            pieEntries.add(new PieEntry(e.getY(), getDayString((int) e.getX())));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "Progress Share");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.BLACK);
        data.setValueTextSize(12f);

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setCenterText("Daily Habit Share");
        pieChart.setVisibility(View.VISIBLE);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void showBarChart(List<Entry> entries) {
        List<BarEntry> barEntries = new ArrayList<>();
        for (Entry e : entries) {
            barEntries.add(new BarEntry(e.getX(), e.getY()));
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "Progress (%)");
        dataSet.setColor(Color.BLUE);
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.setFitBars(true);
        setXAxisLabels(barChart.getXAxis());
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.setVisibility(View.VISIBLE);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void hideAllCharts() {
        lineChart.setVisibility(View.GONE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
    }

    private void setXAxisLabels(com.github.mikephil.charting.components.XAxis xAxis) {
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return getDayString((int) value);
            }
        });
    }

    private String getDayString(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY: return "Sun";
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "";
        }
    }
}
