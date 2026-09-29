package com.mobi.libraryads.commons.GSM;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiGSM {
    @Headers({"Accept: application/json"})
    @POST("/api/auth/login")
    Call<JsonObject> login(
            @Body JsonObject params
    );

    @Headers({"Accept: application/json"})
    @POST("/api/iap/check")
    Call<JsonObject> verifyIAP(
            @Header("Authorization") String token,
            @Body JsonObject params

    );

    @Headers({"Accept: application/json"})
    @POST("/api/iap/subcription/check")
    Call<JsonObject> verifySubs(
            @Header("Authorization") String token,
            @Body JsonObject params

    );

    @Headers({"Accept: application/json"})
    @GET("/api/config")
    Call<JsonObject> getConfig(
            @Query("appId") String appId,
            @Query("deviceId") String deviceId,
            @Query("platform") Integer platform
    );

    @Headers({"Accept: application/json"})
    @GET("/api/app/check-keyhash")
    Call<JsonObject> checkKeyHash(
            @Query("appId") String appId,
            @Query("deviceId") String deviceId,
            @Query("keyhash") String keyhash
    );
}
