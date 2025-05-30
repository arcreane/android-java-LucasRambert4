[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/F-P0wqoO)

# 🌍 TravelBuddy - Smart Travel Companion

**Discover nearby attractions with real-time compass navigation and personalized recommendations**

## ✨ Key Features
- **🧭 Live Compass Navigation** - Visual direction indicator to nearby points of interest
- **🔍 Smart Search & Filters** - Find cafes, museums, parks by category/distance
- **❤️ Favorites System** - Bookmark and organize preferred locations
- **📝 Location Memos** - Add personal notes to saved places
- **🌙 Dark/Light Mode** - Eye-friendly theme switching
- **🌐 Multi-language Support** - English/French localization

## 🛠️ Tech Stack

| Component             | Technology                  |
|-----------------------|-----------------------------|
| **Language**          | Java                      |
| **Architecture**      | MVVM                        |
| **UI Toolkit**        | Jetpack Compose + XML       |
| **Networking**        | Retrofit + Foursquare API   |
| **Database**          | SharedPreferences           |
| **Location**          | FusedLocationProvider       |
| **Dependency Injection** | Manual (No Hilt/Dagger) |

## 📦 Installation

### Prerequisites
- Android Studio Flamingo (2022.2.1) or later
- Android SDK 33 (API 33)
- Google Maps API key

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/TravelBuddy.git
   ```

2. Add your Foursquare API key to `CompassActivity.java`:
   ```java
   private static final String API_KEY = "YOUR_FSQ_API_KEY";
   ```

3. Add your Google Maps API key to `local.properties`:
   ```properties
   MAPS_API_KEY=YOUR_FOURSQUARE_MAPS_KEY
   ```

## 🏗️ Project Structure

```
app/
├── java/
│   ├── adapter/            # RecyclerView adapters
│   ├── api/                # Foursquare API service
│   ├── model/              # Data classes (Place, Category etc.)
│   ├── utils/              # Helper classes
│   └── *.activity          # Activity classes
├── res/
│   ├── layout/             # XML layouts
│   ├── menu/               # Bottom nav items
│   └── values/             # Strings, colors, themes
```

## 🌟 Core Components

### 1. Compass Navigation System
- Real-time bearing calculation using device sensors
- Smooth arrow animation with `RotateAnimation`
- Distance display in km/miles (user configurable)

```java
public void onSensorChanged(SensorEvent event) {
    float degree = Math.round(event.values[0]);
    // Calculate bearing to target location
    double bearing = calculateBearing(currentLat, currentLng, targetLat, targetLng);
    // Animate compass arrow
    animateCompassArrow(degree, bearing);
}
```

### 2. Smart Place Discovery
- Foursquare API integration
- Dynamic filtering by category/distance
- Infinite scroll pagination

```java
Call<PlacesResponse> call = service.searchPlacesWithCategoryIds(
    API_KEY, 
    latLng, 
    query, 
    radius, 
    limit, 
    "DISTANCE", 
    "fsq_id,name,location...", 
    offset,
    categoryIds
);
```

### 3. Favorites System
- Persistent storage using SharedPreferences
- Swipe-to-refresh functionality
- Cross-activity synchronization

```java
// Save favorite
public static void addFavorite(Context context, String fsqId) {
    Set<String> favorites = getFavoritesSet(context);
    favorites.add(fsqId);
    saveFavorites(context, favorites);
}
```

## 🎨 UI Highlights

| Feature              | Implementation                          |
|----------------------|------------------------------------------|
| Bottom Navigation    | BottomNavigationView with 4 tabs         |
| Filter Bottom Sheet  | MaterialBottomSheetDialogFragment        |
| Image Loading        | Glide with rounded corners               |
| Dark Mode            | AppCompatDelegate.setDefaultNightMode()  |
| Localization         | LocaleHelper with resource qualifiers    |

## 📜 License

```
No License

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...
```

## 🤝 Contributing

1. Fork the project  
2. Create your feature branch:  
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. Commit your changes:  
   ```bash
   git commit -m 'Add some amazing feature'
   ```
4. Push to the branch:  
   ```bash
   git push origin feature/AmazingFeature
   ```
5. Open a Pull Request

## ✉️ Contact

For feature requests or bug reports, please open an issue or contact:

📧 **lucasrambert2002@gmail.com**
