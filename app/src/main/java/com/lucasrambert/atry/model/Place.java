package com.lucasrambert.atry.model;

import java.util.List;

public class Place {
    public String fsq_id;
    public String name;
    public Location location;
    public Geocodes geocodes;
    public double distance;
    public List<Category> categories;
    public List<Photo> photos;

    private boolean isFavorite = false;

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getPhotoUrl() {
        if (photos != null && !photos.isEmpty()) {
            Photo best = photos.get(0);
            return best.prefix + "500x500" + best.suffix;
        }
        return null;
    }
}
