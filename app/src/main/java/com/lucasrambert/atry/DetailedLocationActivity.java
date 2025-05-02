package com.lucasrambert.atry;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.FavoriteUtils;
import com.lucasrambert.atry.utils.LocaleHelper;

import java.util.ArrayList;

public class DetailedLocationActivity extends AppCompatActivity {

    private String fsqId;
    private boolean isFavorite = false;
    private ImageButton favoriteButton;
    private TextView categoryView;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    private double targetLat, targetLng;
    private String category;
    private double staticDistance;

    private EditText userMemo;
    private Button saveMemoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_location);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        ArrayList<String> imageUrls = getIntent().getStringArrayListExtra("image_urls");
        category = getIntent().getStringExtra("category");
        staticDistance = getIntent().getDoubleExtra("distance", 0);
        targetLat = getIntent().getDoubleExtra("lat", 0);
        targetLng = getIntent().getDoubleExtra("lng", 0);
        fsqId = getIntent().getStringExtra("fsq_id");

        // View bindings
        TextView nameView = findViewById(R.id.locationName);
        TextView addressView = findViewById(R.id.locationAddress);
        categoryView = findViewById(R.id.categoryDistance);
        GridLayout imageGrid = findViewById(R.id.imageGrid);
        favoriteButton = findViewById(R.id.favoriteButton);
        userMemo = findViewById(R.id.userMemo);
        saveMemoButton = findViewById(R.id.saveMemoButton);

        nameView.setText(name);
        addressView.setText(address);
        categoryView.setText(category + " • " + formatDistance(this, staticDistance));

        // Load and display memo
        SharedPreferences memoPrefs = getSharedPreferences("UserMemos", MODE_PRIVATE);
        String savedMemo = memoPrefs.getString(fsqId, "");
        userMemo.setText(savedMemo);

        // Save memo on button click
        saveMemoButton.setOnClickListener(v -> {
            String note = userMemo.getText().toString().trim();
            SharedPreferences.Editor editor = memoPrefs.edit();
            editor.putString(fsqId, note);
            editor.apply();
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        });

        // Populate 2x2 image grid
        if (imageUrls != null) {
            int count = Math.min(4, imageUrls.size());
            for (int i = 0; i < count; i++) {
                ImageView imageView = new ImageView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(i % 2, 1f);
                params.rowSpec = GridLayout.spec(i / 2, 1f);
                params.setMargins(8, 8, 8, 8);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Glide.with(this)
                        .load(imageUrls.get(i))
                        .placeholder(R.drawable.image_placeholder)
                        .into(imageView);

                imageGrid.addView(imageView);
            }
        }

        updateFavoriteIcon();

        favoriteButton.setOnClickListener(v -> {
            if (isFavorite) {
                FavoriteUtils.removeFavorite(this, fsqId);
                isFavorite = false;
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
            } else {
                FavoriteUtils.addFavorite(this, fsqId);
                isFavorite = true;
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
            }
            updateFavoriteIcon();
        });

        findViewById(R.id.shareButton).setOnClickListener(v -> {
            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + targetLat + "," + targetLng;
            String message = "Check out this place:\n" + name + "\n" + address + "\n\n" + mapsUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        findViewById(R.id.mapsButton).setOnClickListener(v -> {
            String uri = String.format("geo:%f,%f?q=%s", targetLat, targetLng, Uri.encode(name));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homeFragment) {
                finish(); // Go back
                return true;
            } else if (item.getItemId() == R.id.settingsFragment) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        requestLiveDistanceUpdates();
    }

    private void updateFavoriteIcon() {
        isFavorite = FavoriteUtils.isFavorite(this, fsqId);
        favoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite_filled_light : R.drawable.ic_favorite_border_light);
    }

    private void requestLiveDistanceUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest request = LocationRequest.create()
                .setInterval(500)
                .setFastestInterval(500)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location current = result.getLastLocation();
                float[] results = new float[1];
                Location.distanceBetween(
                        current.getLatitude(), current.getLongitude(),
                        targetLat, targetLng, results);
                String updatedDistance = formatDistance(DetailedLocationActivity.this, results[0]);
                categoryView.setText(category + " • " + updatedDistance);
            }
        };

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedClient != null && locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }

        // Auto-save memo on pause
        if (userMemo != null && fsqId != null) {
            SharedPreferences memoPrefs = getSharedPreferences("UserMemos", MODE_PRIVATE);
            SharedPreferences.Editor editor = memoPrefs.edit();
            editor.putString(fsqId, userMemo.getText().toString().trim());
            editor.apply();
        }
    }

    private String formatDistance(Context context, double distanceMeters) {
        SharedPreferences preferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
        String unit = preferences.getString("unitsValue", "Kilometers");

        if ("Miles".equals(unit)) {
            if (distanceMeters >= 1609.34) {
                return String.format("%.1f miles", distanceMeters / 1609.34);
            } else {
                return String.format("%.0f yards", distanceMeters * 1.09361);
            }
        } else {
            if (distanceMeters >= 1000) {
                return String.format("%.1f km", distanceMeters / 1000);
            } else {
                return String.format("%.0f m", distanceMeters);
            }
        }
    }
}
