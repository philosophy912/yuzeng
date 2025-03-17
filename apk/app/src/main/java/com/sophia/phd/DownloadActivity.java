package com.sophia.phd;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

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

    private boolean writeResponseBodyToDisk(ResponseBody body, String fileUrl) {
        try {
            File futureStudioIconFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), getFileNameFromUrl(fileUrl));
            Log.d(TAG, "futureStudioIconFile: " + futureStudioIconFile.getAbsolutePath());
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close(); // 确保在后台线程中关闭
                }
                if (outputStream != null) {
                    outputStream.close(); // 确保在后台线程中关闭
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

    // 新增方法，从URL中提取文件名
    private String getFileNameFromUrl(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        return fileName;
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