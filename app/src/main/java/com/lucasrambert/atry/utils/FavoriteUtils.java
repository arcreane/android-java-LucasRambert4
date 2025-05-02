package com.lucasrambert.atry.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoriteUtils {
    private static final String PREF_NAME = "favorites_prefs";
    private static final String FAVORITES_KEY = "favorite_ids";

    public static void addFavorite(Context context, String fsqId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> favorites = new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
        favorites.add(fsqId);
        prefs.edit().putStringSet(FAVORITES_KEY, favorites).apply();
    }

    public static void removeFavorite(Context context, String fsqId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> favorites = new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
        favorites.remove(fsqId);
        prefs.edit().putStringSet(FAVORITES_KEY, favorites).apply();
    }

    public static boolean isFavorite(Context context, String fsqId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Set<String> favorites = prefs.getStringSet(FAVORITES_KEY, new HashSet<>());
        return favorites != null && favorites.contains(fsqId);
    }

    public static Set<String> getAllFavorites(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
    }
}
