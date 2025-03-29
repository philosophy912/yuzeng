package com.philosophy.translate.api;

import android.util.Log;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://8.137.100.27:8080/";
    private static final Integer TIMEOUT = 300;

    public static ApiService getApiService() {
        Log.d(TAG, "base url is " + BASE_URL);
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT, TimeUnit.SECONDS) // 设置连接超时时间为5分钟
                    .readTimeout(TIMEOUT, TimeUnit.SECONDS)    // 设置读取超时时间为5分钟
                    .writeTimeout(TIMEOUT, TimeUnit.SECONDS)   // 设置写入超时时间为5分钟
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client) // 使用自定义的OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}