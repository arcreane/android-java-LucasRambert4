package com.lucasrambert.atry.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    public static void setLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static void applyLanguage(Context context) {
        SharedPreferencesHelper preferencesHelper = new SharedPreferencesHelper(context);
        String lang = preferencesHelper.getSavedLanguage();
        if (lang != null && !lang.isEmpty()) {
            setLocale(context, lang);
        }
    }
}
