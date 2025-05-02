package com.lucasrambert.atry.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private final SharedPreferences preferences;

    public SharedPreferencesHelper(Context context) {
        preferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
    }

    public String getSavedLanguage() {
        return preferences.getString("languageValue", "English").equals("French") ? "fr" : "en";
    }
}
