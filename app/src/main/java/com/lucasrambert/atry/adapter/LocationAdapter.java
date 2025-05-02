package com.lucasrambert.atry.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lucasrambert.atry.DetailedLocationActivity;
import com.lucasrambert.atry.R;
import com.lucasrambert.atry.model.Place;
import com.lucasrambert.atry.utils.FavoriteUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Place> places;
    private final OnPlaceSelectedListener listener;
    private final boolean openDetails;
    private Location currentLocation;

    public LocationAdapter(List<Place> places, OnPlaceSelectedListener listener, boolean openDetails) {
        this.places = places;
        this.listener = listener;
        this.openDetails = openDetails;
    }

    public void updateData(List<Place> newPlaces) {
        this.places = newPlaces;
        notifyDataSetChanged();
    }

    public void updateCurrentLocation(Location location) {
        this.currentLocation = location;
        notifyDataSetChanged();
    }

    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, distanceText;
        ImageView imageView;
        ImageButton favToggle;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.place_name);
            distanceText = itemView.findViewById(R.id.place_distance);
            imageView = itemView.findViewById(R.id.place_image);
            favToggle = itemView.findViewById(R.id.favoriteToggleButton);
        }
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_placeholder2, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Place place = places.get(position);
        Context context = holder.itemView.getContext();

        holder.nameText.setText(place.name);
        holder.distanceText.setText(formatDistance(context, place));

        Glide.with(context)
                .load(place.getPhotoUrl())
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.image_placeholder)
                .thumbnail(0.25f)
                .circleCrop()
                .into(holder.imageView);

        SharedPreferences preferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean("darkMode", false);

        int filledRes = isDarkMode ? R.drawable.ic_favorite_filled_light : R.drawable.ic_favorite_filled_dark;
        int borderRes = isDarkMode ? R.drawable.ic_favorite_border_light : R.drawable.ic_favorite_border_dark;

        place.setFavorite(FavoriteUtils.isFavorite(context, place.fsq_id));
        holder.favToggle.setImageResource(place.isFavorite() ? filledRes : borderRes);

        holder.favToggle.setOnClickListener(v -> {
            boolean newFavorite = !place.isFavorite();
            place.setFavorite(newFavorite);

            if (newFavorite) {
                FavoriteUtils.addFavorite(context, place.fsq_id);
            } else {
                FavoriteUtils.removeFavorite(context, place.fsq_id);
            }

            holder.favToggle.setImageResource(newFavorite ? filledRes : borderRes);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceSelected(place);
            }
            if (openDetails) {
                Intent intent = new Intent(context, DetailedLocationActivity.class);
                intent.putExtra("name", place.name);
                intent.putExtra("address", place.location != null ? place.location.formatted_address : "No address");
                intent.putExtra("distance", place.distance);
                intent.putExtra("category", place.categories != null && !place.categories.isEmpty() ? place.categories.get(0).name : "Unknown");
                intent.putExtra("fsq_id", place.fsq_id);

                if (place.geocodes != null && place.geocodes.main != null) {
                    intent.putExtra("lat", place.geocodes.main.latitude);
                    intent.putExtra("lng", place.geocodes.main.longitude);
                }

                // ✅ Pass multiple image URLs (max 4)
                if (place.photos != null && !place.photos.isEmpty()) {
                    String[] imageUrls = new String[Math.min(place.photos.size(), 4)];
                    for (int i = 0; i < imageUrls.length; i++) {
                        imageUrls[i] = place.photos.get(i).prefix + "original" + place.photos.get(i).suffix;
                    }
                    intent.putStringArrayListExtra("image_urls", new ArrayList<>(Arrays.asList(imageUrls)));
                }

                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return places != null ? places.size() : 0;
    }

    private String formatDistance(Context context, Place place) {
        if (currentLocation == null || place.geocodes == null || place.geocodes.main == null)
            return "";

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                place.geocodes.main.latitude, place.geocodes.main.longitude,
                results
        );
        double distanceMeters = results[0];

        SharedPreferences preferences = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
        String unit = preferences.getString("unitsValue", "Kilometers");

        if (unit.equals("Miles")) {
            double distanceMiles = distanceMeters / 1609.34;
            return distanceMiles >= 1.0 ? String.format("%.1f miles", distanceMiles) :
                    String.format("%.0f yards", distanceMeters * 1.09361);
        } else {
            return distanceMeters >= 1000 ?
                    String.format("%.1f km", distanceMeters / 1000) :
                    String.format("%.0f m", distanceMeters);
        }
    }
}
