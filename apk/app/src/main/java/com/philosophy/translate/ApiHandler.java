package com.philosophy.translate;

import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.philosophy.translate.api.ApiClient;
import com.philosophy.translate.api.ApiService;
import com.philosophy.translate.api.ApiResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApiHandler {
    private static final String TAG = "ApiHandler";

    public interface ApiHandlerCallback {
        void onSuccess(String message);  // 成功回调

        void onFailure(String error);    // 失败回调
    }

    public static void translate(ApiHandlerCallback callback, File file, String model, String language) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody requestFile = RequestBody.create(file, mediaType);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody modelPart = RequestBody.create(model, MediaType.parse("text/plain"));
        RequestBody languagePart = RequestBody.create(language, MediaType.parse("text/plain"));

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse> call = apiService.translate(body, modelPart, languagePart);
        Log.d(TAG, "访问的URL: " + call.request().url()); // 打印访问的URL

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse uploadResponse = response.body();
                    Log.d(TAG, "Upload successful: " + uploadResponse.getMessage());
//                    callback.onSuccess("翻译成功: " + response.body().getMessage());
                    String url = response.body().getLink();
                    Log.d(TAG, "下载链接: " + url);
                    downloadFile(callback, url, apiService);
                } else {
                    Log.e(TAG, "Upload failed: " + response.message());
                    callback.onFailure("API错误: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Upload failed: " + t.getMessage());
                callback.onFailure("网络错误: " + t.getMessage());
            }
        });

    }

    public static void evaluate(ApiHandlerCallback callback, File file1, File file2, String origin, String target) {
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody requestFile1 = RequestBody.create(file1, mediaType);
        MultipartBody.Part body1 = MultipartBody.Part.createFormData("file1", file1.getName(), requestFile1);

        RequestBody requestFile2 = RequestBody.create(file2, mediaType);
        MultipartBody.Part body2 = MultipartBody.Part.createFormData("file2", file2.getName(), requestFile2);

        RequestBody originPart = RequestBody.create(origin, MediaType.parse("text/plain"));
        RequestBody targetPart = RequestBody.create(target, MediaType.parse("text/plain"));

        ApiService apiService = ApiClient.getApiService();
        Call<ApiResponse> call = apiService.evaluate(body1, body2, originPart, targetPart);
        Log.d(TAG, "访问的URL: " + call.request().url()); // 打印访问的URL
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull retrofit2.Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse uploadResponse = response.body();
                    Log.d(TAG, "Evaluate successful: " + uploadResponse.getMessage());
                    String url = response.body().getLink();
                    Log.d(TAG, "下载链接: " + url);
                    downloadFile(callback, url, apiService);
                    // Handle the response here
                } else {
                    callback.onFailure("API错误: " + response.message());
                    Log.e(TAG, "Evaluate failed: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                callback.onFailure("网络错误: " + t.getMessage());
                Log.e(TAG, "Evaluate failed: " + t.getMessage());
            }
        });
    }

    public static void downloadFile(ApiHandlerCallback callback, String fileUrl, ApiService apiService) {
        Log.d(TAG, "enter downloadFile function");
        Log.d(TAG, "fileUrl is " + fileUrl);
        Call<ResponseBody> call = apiService.downloadFile(fileUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        try (InputStream inputStream = response.body().byteStream()) {
                            // 使用动态获取的下载目录路径
                            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            File outputFile = new File(downloadDir, fileUrl.substring(fileUrl.lastIndexOf('/') + 1));
                            Log.d(TAG, "下载路径: " + outputFile.getAbsolutePath());
                            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                                byte[] buffer = new byte[2048];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                            Log.e(TAG, "文件下载成功");
                            callback.onFailure("文件下载成功");
                        } catch (IOException e) {
                            Log.e(TAG, "下载文件时发生IO错误: " + e.getMessage());
                            callback.onFailure("下载文件时发生IO错误: " + e.getMessage());
                        }
                    }).start();
                } else {
                    String errorMessage = "API错误: " + response.message();
                    Log.e(TAG, errorMessage);
                    callback.onFailure(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                String errorMessage = "网络错误: " + t.getMessage();
                Log.e(TAG, errorMessage);
                callback.onFailure(errorMessage);
            }
        });
    }
}