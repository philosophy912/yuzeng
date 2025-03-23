package com.philosophy.translate;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.philosophy.translate.api.ApiClient;
import com.philosophy.translate.api.ApiService;
import com.philosophy.translate.api.Response;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class ApiHandler {
    private static final String TAG = "ApiHandler";

    public static void translate(Activity activity, File file, String model, String language) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody requestFile = RequestBody.create(file, mediaType);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody modelPart = RequestBody.create(model, MediaType.parse("text/plain"));
        RequestBody languagePart = RequestBody.create(language, MediaType.parse("text/plain"));

        ApiService apiService = ApiClient.getApiService(activity);
        Call<Response> call = apiService.translate(body, modelPart, languagePart);
        call.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(@NonNull Call<Response> call, @NonNull retrofit2.Response<Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response uploadResponse = response.body();
                    Log.d(TAG, "Upload successful: " + uploadResponse.getMessage());
                    // Handle the response here
                } else {
                    Log.e(TAG, "Upload failed: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Response> call, @NonNull Throwable t) {
                Log.e(TAG, "Upload failed: " + t.getMessage());
            }
        });
    }

    public static void evaluate(Activity activity, File file1, File file2) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody requestFile1 = RequestBody.create(file1, mediaType);
        MultipartBody.Part body1 = MultipartBody.Part.createFormData("file1", file1.getName(), requestFile1);

        RequestBody requestFile2 = RequestBody.create(file2, mediaType);
        MultipartBody.Part body2 = MultipartBody.Part.createFormData("file2", file2.getName(), requestFile2);


        ApiService apiService = ApiClient.getApiService(activity);
        Call<Response> call = apiService.evaluate(body1, body2);
        call.enqueue(new Callback<Response>() {
            @Override
            public void onResponse(@NonNull Call<Response> call, @NonNull retrofit2.Response<Response> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Response uploadResponse = response.body();
                    Log.d(TAG, "Evaluate successful: " + uploadResponse.getMessage());
                    // Handle the response here
                } else {
                    Log.e(TAG, "Evaluate failed: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Response> call, @NonNull Throwable t) {
                Log.e(TAG, "Evaluate failed: " + t.getMessage());
            }
        });
    }
}