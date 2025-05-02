package com.lucasrambert.atry;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.adapter.LocationAdapter;
import com.lucasrambert.atry.api.PlacesApiService;
import com.lucasrambert.atry.model.*;
import com.lucasrambert.atry.utils.LocaleHelper;
import retrofit2.*;

import java.util.*;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView compassArrow;
    private TextView selectedLocationInfo;
    private RecyclerView locationList;
    private LocationAdapter locationAdapter;
    private SensorManager sensorManager;
    private float currentDegree = 0f;
    private float currentAzimuth = 0f;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private Location currentLocation = null;

    private final Set<String> categoryFilters = new HashSet<>();
    private List<Place> fullPlaceList = new ArrayList<>();
    private Place selectedPlace = null;
    private int selectedDistance = 1000;

    private static final int LOCATION_PERMISSION_CODE = 101;
    private static final String API_KEY = "fsq3gU0Hu1Zige8xYpdsVbZLvsKTYD9VkbatAunaquDOVRA=";

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
        setContentView(R.layout.activity_compass);

        compassArrow = findViewById(R.id.compassInner);
        selectedLocationInfo = findViewById(R.id.selectedPlaceInfo);
        locationList = findViewById(R.id.locationListRecyclerView);
        Button sortFilterButton = findViewById(R.id.sortFilterButton);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        locationAdapter = new LocationAdapter(new ArrayList<>(), place -> {
            selectedPlace = place;
            Toast.makeText(this, "Selected: " + place.name, Toast.LENGTH_SHORT).show();
        }, false);

        locationList.setLayoutManager(new LinearLayoutManager(this));
        locationList.setAdapter(locationAdapter);

        sortFilterButton.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet();
            sheet.setInitialState(categoryFilters, selectedDistance);
            sheet.setCallback((selectedCategories, distanceMeters) -> {
                categoryFilters.clear();
                categoryFilters.addAll(selectedCategories);
                selectedDistance = distanceMeters;
                fetchPlaces();
            });
            sheet.show(getSupportFragmentManager(), "FilterBottomSheet");
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homeFragment) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.settingsFragment) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(500) // 0.5 seconds
                .setFastestInterval(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                currentLocation = result.getLastLocation();
                locationAdapter.updateCurrentLocation(currentLocation); // ✅ LIVE DISTANCE UPDATE

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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.foursquare.com/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();

        PlacesApiService service = retrofit.create(PlacesApiService.class);
        String latLng = currentLocation.getLatitude() + "," + currentLocation.getLongitude();

        Call<PlacesResponse> call;
        if (!categoryFilters.isEmpty()) {
            String categoriesParam = buildCategoryIds();
            call = service.searchPlacesWithCategoryIds(API_KEY, latLng, "", selectedDistance, 50,
                    "DISTANCE", "fsq_id,name,location,photos,categories,geocodes,distance", 0, categoriesParam);
        } else {
            call = service.searchPlaces(API_KEY, latLng, "", selectedDistance, 50,
                    "DISTANCE", "fsq_id,name,location,photos,categories,geocodes,distance", 0);
        }

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PlacesResponse> call, @NonNull Response<PlacesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullPlaceList = response.body().results;
                    SharedPlacesStore.allLoadedPlaces = new ArrayList<>(fullPlaceList);
                    locationAdapter.updateData(fullPlaceList);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PlacesResponse> call, @NonNull Throwable t) {
                Toast.makeText(CompassActivity.this, "Failed to fetch places", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();
        Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float degree = Math.round(event.values[0]);
        currentAzimuth = degree;

        if (selectedPlace != null && selectedPlace.geocodes != null && selectedPlace.geocodes.main != null && currentLocation != null) {
            double bearing = calculateBearing(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    selectedPlace.geocodes.main.latitude, selectedPlace.geocodes.main.longitude
            );
            float rotation = (float) (bearing - currentAzimuth);

            RotateAnimation rotate = new RotateAnimation(
                    currentDegree, rotation,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(200);
            rotate.setFillAfter(true);

            compassArrow.startAnimation(rotate);
            currentDegree = rotation;

            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    selectedPlace.geocodes.main.latitude, selectedPlace.geocodes.main.longitude,
                    results);

            String distanceText = formatDistance(results[0]);
            selectedLocationInfo.setText("Location: " + selectedPlace.name + "\n" + distanceText);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double x = Math.sin(deltaLon) * Math.cos(lat2Rad);
        double y = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLon);

        return (Math.toDegrees(Math.atan2(x, y)) + 360) % 360;
    }

    private String formatDistance(float distanceMeters) {
        SharedPreferences preferences = getSharedPreferences("SettingsPrefs", MODE_PRIVATE);
        String unit = preferences.getString("unitsValue", "Kilometers");

        if (unit.equals("Miles")) {
            if (distanceMeters >= 1609.34) {
                return String.format("Distance: %.1f miles", distanceMeters / 1609.34);
            } else {
                return String.format("Distance: %.0f yards", distanceMeters * 1.09361);
            }
        } else {
            if (distanceMeters >= 1000) {
                return String.format("Distance: %.1f km", distanceMeters / 1000);
            } else {
                return String.format("Distance: %.0f m", distanceMeters);
            }
        }
    }
}
