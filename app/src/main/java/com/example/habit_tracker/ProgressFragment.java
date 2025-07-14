package com.example.habit_tracker;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProgressFragment extends Fragment {

    private Spinner chartSelector;
    private ProgressBar progressBar;
    private TextView errorText;
    private LineChart lineChart;
    private PieChart pieChart;
    private BarChart barChart;

    private FirebaseFirestore db;
    private String selectedType = "Weekly";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        chartSelector = view.findViewById(R.id.chartSelector);
        progressBar = view.findViewById(R.id.progressBar);
        errorText = view.findViewById(R.id.errorText);
        lineChart = view.findViewById(R.id.lineChart);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        db = FirebaseFirestore.getInstance();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item,
                Arrays.asList("Weekly", "Weekly Pie", "Weekly Bar", "Monthly", "Yearly"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chartSelector.setAdapter(adapter);

        chartSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedType = parent.getItemAtPosition(position).toString();
                loadHabitData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    private void loadHabitData() {
        progressBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        lineChart.setVisibility(View.GONE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);

        db.collection("habits")
                .get()
                .addOnSuccessListener(this::processHabitData)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    errorText.setText("Error loading data");
                    errorText.setVisibility(View.VISIBLE);
                });
    }

    private void processHabitData(QuerySnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        Map<String, Float> dataMap = new LinkedHashMap<>();
        Set<String> labelSet = new LinkedHashSet<>();

        Calendar cal = Calendar.getInstance();
        switch (selectedType) {
            case "Weekly":
            case "Weekly Pie":
            case "Weekly Bar":
                String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                labelSet.addAll(Arrays.asList(days));
                break;
            case "Monthly":
                for (int i = 1; i <= 31; i++) {
                    labelSet.add(String.format(Locale.getDefault(), "%02d %s", i, new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date())));
                }
                break;
            case "Yearly":
                for (int i = 0; i < 12; i++) {
                    cal.set(Calendar.MONTH, i);
                    labelSet.add(new SimpleDateFormat("MMM", Locale.getDefault()).format(cal.getTime()));
                }
                break;
        }

        for (String label : labelSet) {
            dataMap.put(label, 0f);
        }

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long goal = doc.getLong("goal");
            Long current = doc.getLong("currentCount");
            String dateStr = doc.getString("lastUpdatedDate");
            if (goal == null || current == null || dateStr == null || goal == 0) continue;

            float percent = Math.min((current * 100f) / goal, 100f);

            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                cal.setTime(date);
                String key;

                switch (selectedType) {
                    case "Monthly":
                        key = String.format(Locale.getDefault(), "%02d %s", cal.get(Calendar.DAY_OF_MONTH), new SimpleDateFormat("MMM", Locale.getDefault()).format(date));
                        break;
                    case "Yearly":
                        key = new SimpleDateFormat("MMM", Locale.getDefault()).format(date);
                        break;
                    default:
                        key = new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
                        break;
                }

                dataMap.put(key, dataMap.getOrDefault(key, 0f) + percent);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (dataMap.isEmpty()) {
            errorText.setText("No data found");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        if (selectedType.equals("Weekly")) {
            showLineChart(dataMap);
        } else if (selectedType.equals("Weekly Pie") || selectedType.equals("Monthly") || selectedType.equals("Yearly")) {
            showPieChart(dataMap);
        } else if (selectedType.equals("Weekly Bar")) {
            showBarChart(dataMap);
        }
    }

    private void showLineChart(Map<String, Float> data) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : data.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Progress (%)");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setCircleColor(Color.BLUE);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setText("Weekly Progress");

        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = (int) value;
                return (i >= 0 && i < labels.size()) ? labels.get(i) : "";
            }
        });

        lineChart.getAxisLeft().setAxisMaximum(100f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);

        lineChart.setVisibility(View.VISIBLE);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private void showPieChart(Map<String, Float> data) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : data.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, selectedType + " Summary");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setCenterText(selectedType);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setVisibility(View.VISIBLE);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void showBarChart(Map<String, Float> data) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Float> entry : data.entrySet()) {
            entries.add(new BarEntry(i++, entry.getValue()));
            labels.add(entry.getKey());
        }

        BarDataSet dataSet = new BarDataSet(entries, selectedType + " Progress");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < labels.size()) ? labels.get(index) : "";
            }
        });

        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisRight().setEnabled(false);
        barChart.setVisibility(View.VISIBLE);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}