package com.philosophy.translate.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Streaming;

public interface ApiService {
    @Multipart
    @POST("translate")
    Call<ApiResponse> translate(
            @Part MultipartBody.Part file,
            @Part("model") RequestBody model,
            @Part("language") RequestBody language
    );

    @Multipart
    @POST("evaluate")
    Call<ApiResponse> evaluate(
            @Part MultipartBody.Part file1,
            @Part MultipartBody.Part file2,
            @Part("origin") RequestBody origin,
            @Part("target") RequestBody target
    );
    @Streaming
    @GET
    Call<ResponseBody> downloadFile(@retrofit2.http.Url String fileUrl);
}