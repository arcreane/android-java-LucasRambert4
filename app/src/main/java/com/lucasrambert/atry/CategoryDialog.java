package com.lucasrambert.atry;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class CategoryDialog {

    public interface CategorySelectionCallback {
        void onCategorySelected(String category);
    }

    public static void show(Context context, CategorySelectionCallback callback) {
        String[] categories = {"Café", "Museum", "Bar", "Park", "Library", "Restaurant", "No Filter"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Category");
        builder.setItems(categories, (dialog, which) -> {
            String selected = categories[which];
            String category = selected.equalsIgnoreCase("No Filter") ? "" : selected.toLowerCase();
            callback.onCategorySelected(category);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
}
