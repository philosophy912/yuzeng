package com.philosophy.translate.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("translate")
    Call<Response> translate(
            @Part MultipartBody.Part file,
            @Part("model") RequestBody model,
            @Part("language") RequestBody language
    );

    @Multipart
    @POST("evaluate")
    Call<Response> evaluate(
            @Part MultipartBody.Part file1,
            @Part MultipartBody.Part file2
    );
}