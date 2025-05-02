package com.lucasrambert.atry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SimpleCategoryDialog extends DialogFragment {

    public interface CategorySelectCallback {
        void onCategorySelected(String category);
    }

    private CategorySelectCallback callback;

    public SimpleCategoryDialog(CategorySelectCallback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        String[] categories = {"Café", "Museum", "Bar", "Park", "Library", "Restaurant", "No Filter"};

        return new AlertDialog.Builder(context)
                .setTitle("Select Category")
                .setItems(categories, (dialog, which) -> {
                    String selected = categories[which];
                    if (callback != null) {
                        callback.onCategorySelected(selected.equalsIgnoreCase("No Filter") ? "" : selected.toLowerCase());
                    }
                })
                .create();
    }
}
