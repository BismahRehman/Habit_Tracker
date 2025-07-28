package com.example.habit_tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
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
import java.util.Date;
import java.util.Locale;


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

        handleKeyboardVisibility();

        Toolbar toolbar = findViewById(R.id.TopAppBar);
        setSupportActionBar(toolbar);

        // Set current date
        dateText = findViewById(R.id.dateText);
        String currentDate = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date());
        dateText.setText(currentDate);


        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("habit_tracker_channel", "Habit Tracker", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

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

    private void handleKeyboardVisibility() {
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {

        });
    }


}