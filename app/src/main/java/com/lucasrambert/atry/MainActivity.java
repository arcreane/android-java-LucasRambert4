package com.lucasrambert.atry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    Button findPlacesButton;
    BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);

        SharedPreferences preferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean("darkMode", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Change background image dynamically
        ImageView backgroundImage = findViewById(R.id.backgroundImage);
        View overlayView = findViewById(R.id.overlayView);

        if (isDarkMode) {
            backgroundImage.setImageResource(R.drawable.background_main);
        } else {
            backgroundImage.setImageResource(R.drawable.background_main);
        }

        findPlacesButton = findViewById(R.id.findPlacesButton);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // ✅ Only call after initialization
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        findPlacesButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, LocationsActivity.class))
        );

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (itemId == R.id.nav_nearby) {
                startActivity(new Intent(this, CompassActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

}