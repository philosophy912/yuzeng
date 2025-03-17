package com.sophia.phd;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private TextView selectedFileNameTextView;
    private Button startTranslationButton; // 添加开始翻译按钮的引用
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ApiService apiService;
    private String selectFile;
    private ProgressBar progressBar; // 添加ProgressBar来显示Loading界面
    private final Handler handler = new Handler(); // 添加Handler实例
    private Runnable enableButtonsRunnable; // 添加Runnable实例
    private final long buttonEnableDelay = 300000; // 新增变量存放按钮重新启用的时间，5分钟
    private Spinner translationTypeSpinner; // 添加翻译类型Spinner的引用
    private Spinner serviceSpinner; // 添加服务类型Spinner的引用
    private Spinner languageSpinner; // 添加目标语言Spinner的引用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedFileNameTextView = findViewById(R.id.selectedFileName);
        startTranslationButton = findViewById(R.id.button2); // 初始化开始翻译按钮
        startTranslationButton.setEnabled(false); // 设置初始状态为不可用
        progressBar = findViewById(R.id.progressBar); // 初始化ProgressBar
        progressBar.setVisibility(View.GONE); // 默认隐藏ProgressBar

        Button selectFileButton = findViewById(R.id.button1);
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });

        startTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFileNameTextView.getText().toString().equals(getString(R.string.NoFileSelected))) {
                    Toast.makeText(MainActivity.this, "请选择一个文件", Toast.LENGTH_SHORT).show();
                } else {
                    File file = new File(selectFile);
                    Log.d(TAG, "file is " + file.getAbsolutePath());
                    uploadFile(file);

                    // 禁用所有按钮
                    selectFileButton.setEnabled(false);
                    startTranslationButton.setEnabled(false);

                    // 启动一个定时任务，在5分钟后重新启用按钮
                    enableButtonsRunnable = new Runnable() {
                        @Override
                        public void run() {
                            selectFileButton.setEnabled(true);
                            startTranslationButton.setEnabled(true);
                        }
                    };
                    handler.postDelayed(enableButtonsRunnable, buttonEnableDelay); // 使用新变量存放的时间
                }
            }
        });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri uri = data.getData();
                                if (uri != null) {
                                    String filePath = getFilePathFromUri(MainActivity.this, uri);
                                    if (filePath != null) {
                                        String fileName = new File(filePath).getName();
                                        if (isValidFileExtension(fileName)) {
                                            selectFile = saveFileToPrivateStorage(uri);
                                            if (selectFile != null) {
                                                selectedFileNameTextView.setText(fileName);
                                                startTranslationButton.setEnabled(true);
                                            } else {
                                                Toast.makeText(MainActivity.this, "文件拷贝失败", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(MainActivity.this, "不支持的文件类型", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }
                        }
                    }
                });

        // Initialize Retrofit and API service with custom OkHttpClient
        Resources resources = getResources();
        String baseUrl = resources.getString(R.string.server_base_url);

        // 创建OkHttpClient实例并设置超时时间
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Use the base URL from strings.xml
                .client(okHttpClient) // 使用自定义的OkHttpClient
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // 初始化 serviceSpinner
        serviceSpinner = findViewById(R.id.serviceSpinner);

        // 初始化 languageSpinner
        languageSpinner = findViewById(R.id.languageSpinner);

        // 初始化 translationTypeSpinner
        translationTypeSpinner = findViewById(R.id.translationTypeSpinner);
    }

    private void uploadFile(File file) {
        progressBar.setVisibility(View.VISIBLE); // 显示ProgressBar

        // 获取翻译引擎、翻译类型和目标语言的值
        String modelType = ((TextView) serviceSpinner.getSelectedView()).getText().toString();
        String translationType = ((TextView) translationTypeSpinner.getSelectedView()).getText().toString();
        String languageType = ((TextView) languageSpinner.getSelectedView()).getText().toString();

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getName()));
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody modelTypeBody = RequestBody.create(MediaType.parse("text/plain"), modelType);
        RequestBody translationTypeBody = RequestBody.create(MediaType.parse("text/plain"), translationType);
        RequestBody languageTypeBody = RequestBody.create(MediaType.parse("text/plain"), languageType);
        Log.d(TAG, "try to upload file to URL: " + apiService.uploadFile(body, modelTypeBody, translationTypeBody, languageTypeBody).request().url()); // 增加请求URL的日志打印，便于调试
        Call<FileResponse> call = apiService.uploadFile(body, modelTypeBody, translationTypeBody, languageTypeBody);
        call.enqueue(new Callback<FileResponse>() {
            @Override
            public void onResponse(@NonNull Call<FileResponse> call, @NonNull Response<FileResponse> response) {
                progressBar.setVisibility(View.GONE); // 隐藏ProgressBar
                Log.d(TAG, "onResponse"); // 记录响应
                Log.d(TAG, "response: " + response);
                if (response.isSuccessful() && response.body() != null) {
                    FileResponse fileResponse = response.body();
                    Log.d(TAG, "fileResponse: " + fileResponse);
                    if (fileResponse.getStatus() == 20000) {
                        String filePath = fileResponse.getLink();
                        if (filePath != null) {
                            Log.d(TAG, "文件下载地址: " + filePath); // 打印下载地址
                            Intent intent = new Intent(MainActivity.this, DownloadActivity.class);
                            intent.putExtra("fileLink", filePath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "文件上传成功，但未返回文件路径", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "文件上传失败，状态码: " + fileResponse.getStatus(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "文件上传失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<FileResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE); // 隐藏ProgressBar
                Log.d(TAG, "failed by " + t.getMessage()); // 记录失败信息
                Log.e(TAG, "Exception stack trace", t); // 增加异常堆栈信息的日志打印，便于调试
                Toast.makeText(MainActivity.this, "文件上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String saveFileToPrivateStorage(Uri uri) {
        String fileName = getFileNameFromUri(uri); // 获取原始文件名
        File file = new File(getFilesDir(), fileName); // 使用原始文件名保存到私有目录
        try (
                InputStream inputStream = getContentResolver().openInputStream(uri);
                OutputStream outputStream = new FileOutputStream(file)
        ) {
            byte[] buffer = new byte[4 * 1024]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            String absFilePath = file.getAbsolutePath();
            Log.d(TAG, "absFilePath: " + absFilePath);
            return absFilePath; // 返回文件的绝对路径
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }

    private String getFilePathFromUri(Context context, Uri uri) {
        String filePath = null;
        if (uri != null) {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex != -1) {
                            String fileName = cursor.getString(columnIndex);
                            filePath = context.getCacheDir() + "/" + fileName;
                            // 复制文件到缓存目录
                            try {
                                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                                FileOutputStream outputStream = new FileOutputStream(filePath);
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                                inputStream.close();
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                                filePath = null;
                            }
                        }
                    }
                    cursor.close();
                }
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                filePath = uri.getPath();
            }
        }
        return filePath;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain", "text/markdown"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        filePickerLauncher.launch(intent);
    }

    private boolean isValidFileExtension(String fileName) {
        Log.d(TAG, "filename: " + fileName); // 记录文件名
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();
        Log.d(TAG, "File extension: " + fileExtension); // 添加日志，记录文件的扩展名
        return fileExtension.equals("docx") || fileExtension.equals("md") || fileExtension.equals("txt");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除定时任务，避免内存泄漏
        if (enableButtonsRunnable != null) {
            handler.removeCallbacks(enableButtonsRunnable);
        }
    }

    // Define the API service interface
    public interface ApiService {
        @Multipart
        @POST("upload")
            // Replace with your actual endpoint
        Call<FileResponse> uploadFile(@Part MultipartBody.Part file, @Part("modelType") RequestBody modelType, @Part("translationType") RequestBody translationType, @Part("languageType") RequestBody languageType);
    }
}