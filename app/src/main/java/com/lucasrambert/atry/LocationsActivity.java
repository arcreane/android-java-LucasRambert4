package com.lucasrambert.atry;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.adapter.LocationAdapter;
import com.lucasrambert.atry.api.PlacesApiService;
import com.lucasrambert.atry.model.Place;
import com.lucasrambert.atry.model.PlacesResponse;
import com.lucasrambert.atry.utils.LocaleHelper;
import retrofit2.*;

import retrofit2.converter.gson.GsonConverterFactory;
import java.util.*;

public class LocationsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String API_KEY = "fsq3gU0Hu1Zige8xYpdsVbZLvsKTYD9VkbatAunaquDOVRA=";
    private static final int PLACES_PER_REQUEST = 50;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private RecyclerView recyclerView;
    private LocationAdapter locationAdapter;
    private EditText searchInput;
    private ImageButton filterButton;
    private TextView appliedFiltersText;

    private List<Place> fullPlaceList = new ArrayList<>();
    private final Set<String> seenPlaceIds = new HashSet<>();

    private final Set<String> categoryFilters = new HashSet<>();
    private String userQuery = "";
    private int offset = 0;
    private int selectedDistance = 1000;
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

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNav);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        searchInput = findViewById(R.id.searchInput);
        filterButton = findViewById(R.id.filterButton);
        recyclerView = findViewById(R.id.locationList);
        appliedFiltersText = findViewById(R.id.appliedFiltersText);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationAdapter = new LocationAdapter(new ArrayList<>(), null, true);
        recyclerView.setAdapter(locationAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
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
                startActivity(new Intent(this, SettingsActivity.class));
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
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(500)
                .setFastestInterval(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                currentLocation = result.getLastLocation();
                locationAdapter.updateCurrentLocation(currentLocation);

                if (fullPlaceList.isEmpty()) {
                    fetchPlaces();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }

    private void fetchPlaces() {
        if (currentLocation == null) return;

        isLoading = true;
        String latLng = currentLocation.getLatitude() + "," + currentLocation.getLongitude();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.foursquare.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PlacesApiService service = retrofit.create(PlacesApiService.class);

        Call<PlacesResponse> call;
        if (!categoryFilters.isEmpty()) {
            call = service.searchPlacesWithCategoryIds(
                    API_KEY, latLng, userQuery, selectedDistance, PLACES_PER_REQUEST,
                    "DISTANCE", "fsq_id,name,location,photos,categories,geocodes,distance", offset,
                    buildCategoryIds()
            );
        } else {
            call = service.searchPlaces(
                    API_KEY, latLng, userQuery, selectedDistance, PLACES_PER_REQUEST,
                    "DISTANCE", "fsq_id,name,location,photos,categories,geocodes,distance", offset
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
                    fullPlaceList.addAll(newResults);
                    SharedPlacesStore.allLoadedPlaces = new ArrayList<>(fullPlaceList);
                    locationAdapter.updateData(fullPlaceList);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PlacesResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Toast.makeText(LocationsActivity.this, "API Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
    protected void onPause() {
        super.onPause();
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}
