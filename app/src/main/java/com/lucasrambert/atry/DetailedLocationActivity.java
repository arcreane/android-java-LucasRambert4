package com.lucasrambert.atry;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.utils.FavoriteUtils;
import com.lucasrambert.atry.utils.LocaleHelper;

public class DetailedLocationActivity extends AppCompatActivity {

    private String fsqId;
    private boolean isFavorite = false;
    private ImageButton favoriteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_location);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String imageUrl = getIntent().getStringExtra("image");
        String category = getIntent().getStringExtra("category");
        double distance = getIntent().getDoubleExtra("distance", 0);
        double latitude = getIntent().getDoubleExtra("lat", 0);
        double longitude = getIntent().getDoubleExtra("lng", 0);
        fsqId = getIntent().getStringExtra("fsq_id");

        TextView nameView = findViewById(R.id.locationName);
        TextView addressView = findViewById(R.id.locationAddress);
        TextView categoryView = findViewById(R.id.categoryDistance);
        ImageView imageView = findViewById(R.id.locationImage);
        favoriteButton = findViewById(R.id.favoriteButton);

        nameView.setText(name);
        addressView.setText(address);
        categoryView.setText(category + " • " + formatDistance(this, distance));

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.image_placeholder)
                .into(imageView);

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
            String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
            String message = "Check out this place:\n" + name + "\n" + address + "\n\n" + mapsUrl;

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        findViewById(R.id.mapsButton).setOnClickListener(v -> {
            String uri = String.format("geo:%f,%f?q=%s", latitude, longitude, Uri.encode(name));
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
    }

    private void updateFavoriteIcon() {
        isFavorite = FavoriteUtils.isFavorite(this, fsqId);
        favoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite_filled_light : R.drawable.ic_favorite_border_light);
    }

    // 🔥 Format the distance based on user settings (Kilometers/Miles)
    private String formatDistance(Context context, double distanceMeters) {
        SharedPreferences preferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
        String unit = preferences.getString("unitsValue", "Kilometers");

        if (unit.equals("Miles")) {
            double distanceMiles = distanceMeters / 1609.34;
            if (distanceMiles >= 1.0) {
                return String.format("%.1f miles", distanceMiles);
            } else {
                double distanceYards = distanceMeters * 1.09361;
                return String.format("%.0f yards", distanceYards);
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
