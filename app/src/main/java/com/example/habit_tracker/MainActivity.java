package com.example.habit_tracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.app.DatePickerDialog;
import android.widget.DatePicker;

public class MainActivity extends AppCompatActivity {


    TextView dateText;
    ImageView chatIcon;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Toolbar toolbar = findViewById(R.id.TopAppBar);
        setSupportActionBar(toolbar);

        // Set current date
        dateText = findViewById(R.id.dateText);
        String currentDate = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date());
        dateText.setText(currentDate);

        dateText.setOnClickListener(v -> {
            // Get current date
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Show date picker
            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format selected date and show it
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        String formattedDate = new SimpleDateFormat("MMM d", Locale.getDefault()).format(selectedDate.getTime());
                        dateText.setText(formattedDate);
                    }, year, month, day);

            datePickerDialog.show();
        });

        // Handle chat icon click
        chatIcon = findViewById(R.id.chatIcon);
        chatIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            startActivity(intent);
        });



        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();
        }
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            loadFragment(new HomeFragment());
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;

            } else if (id == R.id.nav_noted) {
                loadFragment(new NotesFragment());
                return true;

            } else if (id == R.id.nav_fab) {
                loadFragment(new FabFragment());
                return true;

            } else if (id == R.id.nav_progress) {
                loadFragment(new ProgressFragment());
                return true;

            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment());                    return true;
            }
            return false;
        });

    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
}