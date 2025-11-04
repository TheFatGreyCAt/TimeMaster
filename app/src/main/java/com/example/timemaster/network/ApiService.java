package com.example.timemaster.network;

import com.example.timemaster.model.TimestampResponse;
import com.example.timemaster.model.WorldTimeResponse;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/time/current/zone?timeZone=Asia/Ho_Chi_Minh")
    Call<TimestampResponse> getCurrentTimestamp();
//  api/Time/current

    @GET("api/timezone/Asia/Ho_Chi_Minh")
    Call<WorldTimeResponse> getWorldTime();
}
