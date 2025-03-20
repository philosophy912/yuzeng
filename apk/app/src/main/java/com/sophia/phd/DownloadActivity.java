package com.sophia.phd;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.view.View;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Environment;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;
import retrofit2.http.Streaming;
import retrofit2.http.GET;

public class DownloadActivity extends AppCompatActivity {

    private final String TAG = DownloadActivity.class.getSimpleName();

    private TextView downloadStatusTextView;
    private Button downloadButton;
    private ProgressBar downloadProgressBar;
    private ApiService apiService;
    private String fileLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);

        downloadStatusTextView = findViewById(R.id.downloadStatus);
        downloadButton = findViewById(R.id.downloadButton);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);

        fileLink = getIntent().getStringExtra("fileLink");

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFile();
            }
        });

        // Initialize Retrofit and API service
        Resources resources = getResources();
        String baseUrl = resources.getString(R.string.server_base_url);
        Retrofit retrofit = new Retrofit.Builder().baseUrl(baseUrl) // Use the base URL from strings.xml
                .addConverterFactory(GsonConverterFactory.create()).build();

        apiService = retrofit.create(ApiService.class);
    }

    private void downloadFile() {
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadStatusTextView.setText("开始下载...");
        downloadButton.setEnabled(false); // 禁用下载按钮

        Call<ResponseBody> call = apiService.downloadFile(fileLink);
        handleTimeout(call); // 调用超时处理方法

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 将文件写入操作移到后台线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body(), fileLink);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (writtenToDisk) {
                                        downloadStatusTextView.setText("下载完成");
                                        // 弹出下载文件存放的地址
                                        File futureStudioIconFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getFileNameFromUrl(fileLink));
                                        Log.d(TAG, "文件下载地址: " + futureStudioIconFile.getAbsolutePath());
                                        Toast toast = Toast.makeText(DownloadActivity.this, "文件下载地址: " + futureStudioIconFile.getAbsolutePath(), Toast.LENGTH_LONG);
                                        toast.setDuration(Toast.LENGTH_SHORT); // 设置Toast显示时间为5秒
                                        toast.show();
                                        // 使用Handler延迟跳转
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent(DownloadActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish(); // 关闭当前活动
                                            }
                                        }, 5000); // 延迟时间为5秒
                                    } else {
                                        downloadStatusTextView.setText("下载失败");
                                    }
                                    downloadProgressBar.setVisibility(View.GONE);
                                    downloadButton.setEnabled(true); // 重新启用下载按钮
                                }
                            });
                        }
                    }).start();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            downloadStatusTextView.setText("下载失败");
                            downloadProgressBar.setVisibility(View.GONE);
                            downloadButton.setEnabled(true); // 重新启用下载按钮
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadStatusTextView.setText("下载失败: " + t.getMessage());
                        downloadProgressBar.setVisibility(View.GONE);
                        downloadButton.setEnabled(true); // 重新启用下载按钮
                        Log.e("DownloadActivity", "Exception stack trace", t);
                    }
                });
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private boolean writeResponseBodyToDisk(ResponseBody body, String fileUrl) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            String fileName = getFileNameFromUrl(fileUrl);
            if (TextUtils.isEmpty(fileName)) {
                Log.e(TAG, "Invalid file name derived from URL.");
                return false;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // For Android 10 (API 29) and above, use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri == null) {
                    Log.e(TAG, "Failed to create new MediaStore record.");
                    return false;
                }

                outputStream = resolver.openOutputStream(uri);
            } else {
                // For Android 9 (API 28) and below, use traditional file system
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                    Log.e(TAG, "Failed to create downloads directory.");
                    return false;
                }

                File destinationFile = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(destinationFile);
            }

            if (outputStream == null) {
                Log.e(TAG, "Failed to open output stream.");
                return false;
            }

            inputStream = body.byteStream();
            byte[] fileReader = new byte[4096];
            long fileSize = body.contentLength();
            long fileSizeDownloaded = 0;

            int read;
            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(fileReader, 0, read);
                fileSizeDownloaded += read;
                Log.d(TAG, "File download progress: " + fileSizeDownloaded + " of " + fileSize);
            }

            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output stream: " + e.getMessage());
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams: " + e.getMessage());
            }
        }
    }

    // 新增方法，从URL中提取文件名
    private String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        return fileName;
    }

    // 新增方法，获取文件MIME类型
    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        if (type == null) {
            type = "application/octet-stream"; // 默认MIME类型
        }
        return type;
    }

    // 新增超时处理逻辑
    private void handleTimeout(Call<ResponseBody> call) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300000); // 5分钟
                    if (!call.isExecuted() || call.isCanceled()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "download timeout 5 minutes");
                                downloadStatusTextView.setText("下载超时");
                                downloadProgressBar.setVisibility(View.GONE);
                                downloadButton.setEnabled(true); // 重新启用下载按钮
                            }
                        });
                    } else if (call.isExecuted() && !call.isCanceled()) {
                        call.cancel(); // 取消请求
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "download timeout 5 minutes");
                                downloadStatusTextView.setText("下载超时");
                                downloadProgressBar.setVisibility(View.GONE);
                                downloadButton.setEnabled(true); // 重新启用下载按钮
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // Define the API service interface
    public interface ApiService {
        @Streaming
        @GET
        Call<ResponseBody> downloadFile(@retrofit2.http.Url String fileUrl);
    }
}