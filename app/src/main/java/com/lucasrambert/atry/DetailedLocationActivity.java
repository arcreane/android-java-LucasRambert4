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
import android.view.View;
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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.location.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.FavoriteUtils;
import com.lucasrambert.atry.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private TextView savedMemoView;
    private Button saveMemoButton, clearMemoButton, modifyMemoButton, createMemoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_location);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");

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
        savedMemoView = findViewById(R.id.savedMemoView);
        saveMemoButton = findViewById(R.id.saveMemoButton);
        clearMemoButton = findViewById(R.id.clearMemoButton);
        modifyMemoButton = findViewById(R.id.modifyMemoButton);
        createMemoButton = findViewById(R.id.createMemoButton);

        nameView.setText(name);
        addressView.setText(address);
        categoryView.setText(category + " • " + formatDistance(this, staticDistance));

        SharedPreferences memoPrefs = getSharedPreferences("UserMemos", MODE_PRIVATE);
        String savedMemo = memoPrefs.getString(fsqId, "");

        if (!savedMemo.isEmpty()) {
            savedMemoView.setText(savedMemo);
            savedMemoView.setVisibility(View.VISIBLE);
            modifyMemoButton.setVisibility(View.VISIBLE);
            clearMemoButton.setVisibility(View.VISIBLE);
        } else {
            createMemoButton.setVisibility(View.VISIBLE);
        }

        createMemoButton.setOnClickListener(v -> {
            userMemo.setText("");
            userMemo.setVisibility(View.VISIBLE);
            saveMemoButton.setVisibility(View.VISIBLE);
            createMemoButton.setVisibility(View.GONE);
        });

        modifyMemoButton.setOnClickListener(v -> {
            userMemo.setText(savedMemoView.getText().toString());
            userMemo.setVisibility(View.VISIBLE);
            saveMemoButton.setVisibility(View.VISIBLE);
            savedMemoView.setVisibility(View.GONE);
            modifyMemoButton.setVisibility(View.GONE);
        });

        saveMemoButton.setOnClickListener(v -> {
            String note = userMemo.getText().toString().trim();
            SharedPreferences.Editor editor = memoPrefs.edit();
            editor.putString(fsqId, note);
            editor.apply();

            savedMemoView.setText(note);
            savedMemoView.setVisibility(View.VISIBLE);
            userMemo.setVisibility(View.GONE);
            saveMemoButton.setVisibility(View.GONE);
            modifyMemoButton.setVisibility(View.VISIBLE);
            clearMemoButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        });

        clearMemoButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = memoPrefs.edit();
            editor.remove(fsqId);
            editor.apply();

            userMemo.setText("");
            savedMemoView.setText("");
            savedMemoView.setVisibility(View.GONE);
            userMemo.setVisibility(View.GONE);
            saveMemoButton.setVisibility(View.GONE);
            clearMemoButton.setVisibility(View.GONE);
            modifyMemoButton.setVisibility(View.GONE);
            createMemoButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Note cleared", Toast.LENGTH_SHORT).show();
        });

        // In your Activity's onCreate()
        ArrayList<String> imageUrls = getIntent().getStringArrayListExtra("image_urls");

        ImageView image1 = findViewById(R.id.image1);
        ImageView image2 = findViewById(R.id.image2);
        ImageView image3 = findViewById(R.id.image3);
        ImageView image4 = findViewById(R.id.image4);

        List<ImageView> imageViews = Arrays.asList(image1, image2, image3, image4);

        if (imageUrls != null) {
            int count = Math.min(4, imageUrls.size());
            for (int i = 0; i < count; i++) {
                RequestOptions options = new RequestOptions()
                        .transform(new RoundedCorners(16)); // 16px corner radius

                Glide.with(this)
                        .load(imageUrls.get(i))
                        .apply(options)
                        .placeholder(R.drawable.image_placeholder)
                        .into(imageViews.get(i));
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

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
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

        // Auto-save memo if visible
        if (userMemo != null && userMemo.getVisibility() == View.VISIBLE && fsqId != null) {
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
