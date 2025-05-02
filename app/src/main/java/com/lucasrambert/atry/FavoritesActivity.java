package com.lucasrambert.atry;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lucasrambert.atry.adapter.LocationAdapter;
import com.lucasrambert.atry.adapter.OnPlaceSelectedListener;
import com.lucasrambert.atry.model.Place;
import com.lucasrambert.atry.utils.FavoriteUtils;
import com.lucasrambert.atry.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FavoritesActivity extends AppCompatActivity {

    private LocationAdapter locationAdapter;
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Place> favoritePlaces = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applyLanguage(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        emptyMessage = findViewById(R.id.emptyMessage);
        recyclerView = findViewById(R.id.locationList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        locationAdapter = new LocationAdapter(favoritePlaces, place -> {
            // Handle place click (open details if needed)
        }, true); // openDetails = true

        recyclerView.setAdapter(locationAdapter);

        // Load favorites first time
        loadFavorites();

        // Pull to refresh logic
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadFavorites();
            swipeRefreshLayout.setRefreshing(false);
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

    private void loadFavorites() {
        favoritePlaces.clear();
        Set<String> favoriteIds = FavoriteUtils.getAllFavorites(this);

        for (Place p : SharedPlacesStore.allLoadedPlaces) {
            if (favoriteIds.contains(p.fsq_id)) {
                p.setFavorite(true); // very important to reflect favorite visually
                favoritePlaces.add(p);
            }
        }

        locationAdapter.updateData(favoritePlaces);

        if (favoritePlaces.isEmpty()) {
            emptyMessage.setVisibility(TextView.VISIBLE);
        } else {
            emptyMessage.setVisibility(TextView.GONE);
        }
    }
}
