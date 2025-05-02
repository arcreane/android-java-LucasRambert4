package com.lucasrambert.atry;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.LocaleHelper;

public class SettingsActivity extends AppCompatActivity {

    private Switch darkModeSwitch, notificationsSwitch;
    private Spinner unitsSpinner, languageSpinner;
    private TextView permissionsLink, termsLink;
    private BottomNavigationView bottomNavigation;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
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
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        permissionsLink = findViewById(R.id.permissionsLink);
        termsLink = findViewById(R.id.termsLink);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Dark Mode
        boolean isDarkMode = preferences.getBoolean("darkMode", false);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("darkMode", isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

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
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
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

                if (selectedLanguage.equals("French")) {
                    LocaleHelper.setLocale(SettingsActivity.this, "fr");
                } else {
                    LocaleHelper.setLocale(SettingsActivity.this, "en");
                }

                Toast.makeText(SettingsActivity.this, "Language set to " + selectedLanguage, Toast.LENGTH_SHORT).show();
                restartActivity();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Notifications
        notificationsSwitch.setChecked(preferences.getBoolean("notifications", true));
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notifications", isChecked);
            editor.apply();

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                REQUEST_NOTIFICATION_PERMISSION);
                    }
                }
            }
        });

        // Permissions link
        permissionsLink.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        // Terms link
        termsLink.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://yourtermswebsite.com")); // <-- Your URL
            startActivity(browserIntent);
        });

        // Bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homeFragment) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.settingsFragment) {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
