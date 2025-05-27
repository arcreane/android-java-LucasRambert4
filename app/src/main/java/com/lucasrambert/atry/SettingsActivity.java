package com.lucasrambert.atry;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.LocaleHelper;

public class SettingsActivity extends AppCompatActivity {

    private Switch darkModeSwitch;
    private Spinner unitsSpinner, languageSpinner;
    private TextView permissionsLink, termsLink;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private boolean isFirstLanguageSelection = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);
        editor = preferences.edit();

        // Find views
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        unitsSpinner = findViewById(R.id.unitsSpinner);
        languageSpinner = findViewById(R.id.languageSpinner);
        permissionsLink = findViewById(R.id.permissionsLink);
        termsLink = findViewById(R.id.termsLink);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_settings);

        // Dark Mode
        boolean isDarkMode = preferences.getBoolean("darkMode", false);
        darkModeSwitch.setChecked(isDarkMode);
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("darkMode", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            restartActivity();
        });

        // Units Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.units_array,
                R.layout.spinner_item
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        unitsSpinner.setAdapter(adapter);
        unitsSpinner.setSelection(preferences.getInt("unitsPosition", 0));
        unitsSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                editor.putInt("unitsPosition", position);
                editor.putString("unitsValue", parent.getItemAtPosition(position).toString());
                editor.apply();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Language Spinner
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                new String[]{"English", "French"}
        );
        languageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        languageSpinner.setSelection(preferences.getInt("languagePosition", 0));
        languageSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (isFirstLanguageSelection) {
                    isFirstLanguageSelection = false;
                    return;
                }

                String selectedLanguage = (String) parent.getItemAtPosition(position);
                editor.putInt("languagePosition", position);
                editor.putString("languageValue", selectedLanguage);
                editor.apply();

                LocaleHelper.setLocale(SettingsActivity.this, selectedLanguage.equals("French") ? "fr" : "en");
                Toast.makeText(SettingsActivity.this, "Language set to " + selectedLanguage, Toast.LENGTH_SHORT).show();
                restartActivity();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Permissions link
        permissionsLink.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        // Terms link
        termsLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://yourtermswebsite.com"));
            startActivity(browserIntent);
        });

        // Bottom navigation
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
                return true;
            }
            return false;
        });
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
