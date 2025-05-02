package com.lucasrambert.atry.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

import com.lucasrambert.atry.model.PlacesResponse;

public interface PlacesApiService {
    @GET("v3/places/search")
    Call<PlacesResponse> searchPlaces(
            @Header("Authorization") String authHeader,
            @Query("ll") String latLng,
            @Query("query") String query,
            @Query("radius") int radius,
            @Query("limit") int limit,
            @Query("sort") String sort,
            @Query("fields") String fields,
            @Query("offset") int offset
    );

    @GET("v3/places/search")
    Call<PlacesResponse> searchPlacesWithCategoryIds(
            @Header("Authorization") String apiKey,
            @Query("ll") String latLng,
            @Query("query") String query,
            @Query("radius") int radius,
            @Query("limit") int limit,
            @Query("sort") String sort,
            @Query("fields") String fields,
            @Query("offset") int offset,
            @Query("categories") String categoryIds // <-- NEW PARAM
    );

}

