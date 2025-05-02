package com.lucasrambert.atry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    Button findPlacesButton;
    LinearLayout nearbySection, favoritesSection, shareSection;
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

        // 🔥 Change background image dynamically
        ImageView backgroundImage = findViewById(R.id.backgroundImage);
        View overlayView = findViewById(R.id.overlayView);

        if (isDarkMode) {
            backgroundImage.setImageResource(R.drawable.people_walking_darkmode);
        } else {
            backgroundImage.setImageResource(R.drawable.people_walking);
        }


        findPlacesButton = findViewById(R.id.findPlacesButton);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        LinearLayout iconRow = findViewById(R.id.iconRow);
        nearbySection = (LinearLayout) iconRow.getChildAt(0);
        favoritesSection = (LinearLayout) iconRow.getChildAt(1);
        shareSection = (LinearLayout) iconRow.getChildAt(2);

        findPlacesButton.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, LocationsActivity.class))
        );

        nearbySection.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, CompassActivity.class))
        );

        favoritesSection.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, FavoritesActivity.class))
        );

        shareSection.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this cool place-finding app!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.homeFragment) {
                // Already in home page, do nothing or just return true
                return true;
            } else if (itemId == R.id.settingsFragment) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }
}
