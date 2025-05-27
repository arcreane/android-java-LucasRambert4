package com.lucasrambert.atry;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.adapter.LocationAdapter;
import com.lucasrambert.atry.model.Place;
import com.lucasrambert.atry.utils.FavoriteUtils;
import com.lucasrambert.atry.utils.LocaleHelper;

import java.util.*;

public class FavoritesActivity extends AppCompatActivity {

    private LocationAdapter locationAdapter;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Place> favoritePlaces = new ArrayList<>();

    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNav);
        bottomNavigation.setSelectedItemId(R.id.nav_favorites);

        emptyMessage = findViewById(R.id.emptyMessage);
        recyclerView = findViewById(R.id.locationList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        locationAdapter = new LocationAdapter(favoritePlaces, place -> {}, true);
        recyclerView.setAdapter(locationAdapter);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        fetchUserLocation();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserLocation();
            swipeRefreshLayout.setRefreshing(false);
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
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void fetchUserLocation() {
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                locationAdapter.updateCurrentLocation(location);
            }
            loadFavorites();
        });
    }

    private void loadFavorites() {
        favoritePlaces.clear();
        Set<String> favoriteIds = FavoriteUtils.getAllFavorites(this);

        for (Place p : SharedPlacesStore.allLoadedPlaces) {
            if (favoriteIds.contains(p.fsq_id)) {
                p.setFavorite(true);
                favoritePlaces.add(p);
            }
        }

        locationAdapter.updateData(favoritePlaces);

        emptyMessage.setVisibility(favoritePlaces.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }
}
