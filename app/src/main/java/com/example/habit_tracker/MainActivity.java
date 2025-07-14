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


        Toolbar toolbar = findViewById(R.id.TopAppBar);
        setSupportActionBar(toolbar);

        // Set current date
        dateText = findViewById(R.id.dateText);
        String currentDate = new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date());
        dateText.setText(currentDate);

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