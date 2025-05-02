package com.lucasrambert.atry;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.adapter.LocationAdapter;
import com.lucasrambert.atry.api.PlacesApiService;
import com.lucasrambert.atry.model.Place;
import com.lucasrambert.atry.model.PlacesResponse;
import com.lucasrambert.atry.utils.LocaleHelper;

import java.util.*;
import retrofit2.*;

import retrofit2.converter.gson.GsonConverterFactory;

public class LocationsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String API_KEY = "fsq3gU0Hu1Zige8xYpdsVbZLvsKTYD9VkbatAunaquDOVRA=";
    private static final int PLACES_PER_REQUEST = 50;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private RecyclerView recyclerView;
    private LocationAdapter locationAdapter;
    private EditText searchInput;
    private ImageButton filterButton;
    private TextView appliedFiltersText;

    private List<Place> fullPlaceList = new ArrayList<>();
    private final Set<String> seenPlaceIds = new HashSet<>();

    private final Set<String> categoryFilters = new HashSet<>();
    private String userQuery = "";
    private String currentLatLng = "";
    private int offset = 0;
    private int selectedDistance = 1000; // meters
    private boolean isLoading = false;

    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
    static {
        CATEGORY_MAP.put("café", "13032");
        CATEGORY_MAP.put("museum", "10027");
        CATEGORY_MAP.put("bar", "13003");
        CATEGORY_MAP.put("park", "16032");
        CATEGORY_MAP.put("library", "12042");
        CATEGORY_MAP.put("restaurant", "13065");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        searchInput = findViewById(R.id.searchInput);
        filterButton = findViewById(R.id.filterButton);
        recyclerView = findViewById(R.id.locationList);
        appliedFiltersText = findViewById(R.id.appliedFiltersText);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationAdapter = new LocationAdapter(new ArrayList<>(), null, true); // true = open detail
        recyclerView.setAdapter(locationAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (!recyclerView.canScrollVertically(1) && !isLoading && categoryFilters.isEmpty()) {
                    offset += PLACES_PER_REQUEST;
                    fetchPlaces();
                }
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            userQuery = searchInput.getText().toString();
            offset = 0;
            seenPlaceIds.clear();
            fullPlaceList.clear();
            fetchPlaces();
            return true;
        });

        filterButton.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet();
            sheet.setInitialState(categoryFilters, selectedDistance);
            sheet.setCallback((selectedCategories, selectedDistanceMeters) -> {
                categoryFilters.clear();
                categoryFilters.addAll(selectedCategories);
                selectedDistance = selectedDistanceMeters;
                offset = 0;
                fullPlaceList.clear();
                seenPlaceIds.clear();
                updateFilterSummary();
                fetchPlaces();
            });
            sheet.show(getSupportFragmentManager(), "FilterBottomSheet");
        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homeFragment) {
                finish();
                return true;
            } else if (item.getItemId() == R.id.settingsFragment) {
                startActivity(new android.content.Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        requestLocationPermission();
    }

    private void updateFilterSummary() {
        StringBuilder sb = new StringBuilder();
        if (!categoryFilters.isEmpty()) {
            List<String> capitalized = new ArrayList<>();
            for (String cat : categoryFilters) {
                capitalized.add(Character.toUpperCase(cat.charAt(0)) + cat.substring(1));
            }
            sb.append("Categories: ").append(String.join(", ", capitalized)).append(" | ");
        }
        sb.append("Distance: ").append(selectedDistance / 1000).append(" km");
        appliedFiltersText.setText(sb.toString());
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLatLng = location.getLatitude() + "," + location.getLongitude();
                    fetchPlaces();
                }
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location access denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPlaces() {
        if (currentLatLng.isEmpty()) return;
        isLoading = true;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.foursquare.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PlacesApiService service = retrofit.create(PlacesApiService.class);

        Call<PlacesResponse> call;

        if (!categoryFilters.isEmpty()) {
            String categoriesParam = buildCategoryIds();
            call = service.searchPlacesWithCategoryIds(
                    API_KEY,
                    currentLatLng,
                    userQuery,
                    selectedDistance,
                    PLACES_PER_REQUEST,
                    "DISTANCE",
                    "fsq_id,name,location,photos,categories,geocodes,distance",
                    offset,
                    categoriesParam
            );
        } else {
            call = service.searchPlaces(
                    API_KEY,
                    currentLatLng,
                    userQuery,
                    selectedDistance,
                    PLACES_PER_REQUEST,
                    "DISTANCE",
                    "fsq_id,name,location,photos,categories,geocodes,distance",
                    offset
            );
        }

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PlacesResponse> call, @NonNull Response<PlacesResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Place> results = response.body().results;

                    List<Place> newResults = new ArrayList<>();
                    for (Place place : results) {
                        if (!seenPlaceIds.contains(place.fsq_id)) {
                            seenPlaceIds.add(place.fsq_id);
                            newResults.add(place);
                        }
                    }

                    if (newResults.isEmpty()) {
                        Toast.makeText(LocationsActivity.this, "No new places found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    fullPlaceList.addAll(newResults);
                    SharedPlacesStore.allLoadedPlaces = new ArrayList<>(fullPlaceList);
                    locationAdapter.updateData(fullPlaceList);
                } else {
                    Toast.makeText(LocationsActivity.this, "No more places", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PlacesResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Toast.makeText(LocationsActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    private String buildCategoryIds() {
        List<String> ids = new ArrayList<>();
        for (String cat : categoryFilters) {
            if (CATEGORY_MAP.containsKey(cat)) {
                ids.add(CATEGORY_MAP.get(cat));
            }
        }
        return String.join(",", ids);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
}
