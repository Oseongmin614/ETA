package com.example.eta.service;
import com.example.eta.model.NerRequest;
import com.example.eta.model.NerResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
public interface NerApi {
    @GET("/test")
    Call<String> testConnection();

    @POST("/ner")
    Call<List<String>> getNerResult(@Body NerRequest request);
}
