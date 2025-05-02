// FilterBottomSheet.java
package com.lucasrambert.atry;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.HashSet;
import java.util.Set;


public class FilterBottomSheet extends BottomSheetDialogFragment {

    public interface FilterCallback {
        void onFilterSelected(Set<String> selectedCategories, int distanceMeters);
    }

    private FilterCallback callback;
    private final Set<String> selectedCategories = new HashSet<>();
    private int selectedDistance = 1000;

    private Set<String> preselectedCategories = new HashSet<>();
    private int initialDistance = 1000;

    public void setInitialState(Set<String> selectedCategories, int distance) {
        this.preselectedCategories = new HashSet<>(selectedCategories);
        this.initialDistance = distance;
    }


    public void setCallback(FilterCallback callback) {
        this.callback = callback;
    }

    public void setInitialFilters(Set<String> categories, int distance) {
        selectedCategories.clear();
        selectedCategories.addAll(categories);
        selectedDistance = distance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.bottom_sheet_filter, container, false);

        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        TextView distanceValue = view.findViewById(R.id.distanceValue);
        Slider distanceSlider = view.findViewById(R.id.distanceSlider);

        distanceSlider.setValue(selectedDistance / 1000f);
        distanceValue.setText((int) distanceSlider.getValue() + " km");

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            selectedDistance = (int) value * 1000;
            distanceValue.setText((int) value + " km");
        });

        String[] categories = {"Café", "Museum", "Bar", "Park", "Library", "Restaurant"};

        for (String label : categories) {
            Chip chip = new Chip(requireContext());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(selectedCategories.contains(label.toLowerCase()));

            chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            chip.setChipBackgroundColorResource(android.R.color.darker_gray);

            chip.setOnCheckedChangeListener((btn, isChecked) -> {
                String cat = label.toLowerCase();
                if (isChecked) {
                    selectedCategories.add(cat);
                } else {
                    selectedCategories.remove(cat);
                }
            });

            chipGroup.addView(chip);
        }

        view.findViewById(R.id.applyFilterButton).setOnClickListener(v -> {
            if (callback != null) {
                callback.onFilterSelected(new HashSet<>(selectedCategories), selectedDistance);
            }
            dismiss();
        });

        return view;
    }
}

