package com.philosophy.translate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class TranslateActivity extends AppCompatActivity implements ApiHandler.ApiHandlerCallback {

    private static final String TAG = "TranslateActivity";
    private FileSelectorHelper translateFileSelectorHelper;
    private Spinner modelSpinner;
    private Spinner languageSpinner;
    private ProgressBar progressBar;

    private static final Integer delayTimes = 5000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

        // 初始化进度条
        progressBar = findViewById(R.id.progressBar);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TranslateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button selectFileButton = findViewById(R.id.selectFileButton);
        TextView selectedTranslateFileNameTextView = findViewById(R.id.selectedTranslateFileName);

        translateFileSelectorHelper = new FileSelectorHelper(TranslateActivity.this, selectedTranslateFileNameTextView);
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateFileSelectorHelper.showFileChooser();
            }
        });

        Button startTranslateButton = findViewById(R.id.startTranslateButton);
        modelSpinner = findViewById(R.id.modelSpinner);
        languageSpinner = findViewById(R.id.languageSpinner);

        startTranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取选中的文件、模型和语言
                File file = translateFileSelectorHelper.getSelectFile();
                String model = modelSpinner.getSelectedItem().toString();
                String language = languageSpinner.getSelectedItem().toString();

                // 禁用按钮并显示进度条
                disableButtons();
                progressBar.setVisibility(View.VISIBLE);

                // 调用API进行翻译
                ApiHandler.translate(TranslateActivity.this, file, model, language);

            }
        });
    }

    // 新增方法：禁用所有按钮
    private void disableButtons() {
        Button backButton = findViewById(R.id.backButton);
        Button selectFileButton = findViewById(R.id.selectFileButton);
        Button startTranslateButton = findViewById(R.id.startTranslateButton);

        backButton.setEnabled(false);
        selectFileButton.setEnabled(false);
        startTranslateButton.setEnabled(false);
        modelSpinner.setEnabled(false); // 新增：禁用模型Spinner
        languageSpinner.setEnabled(false); // 新增：禁用语言Spinner
    }

    private void enableButtons() {
        Button backButton = findViewById(R.id.backButton);
        Button selectFileButton = findViewById(R.id.selectFileButton);
        Button startTranslateButton = findViewById(R.id.startTranslateButton);

        backButton.setEnabled(true);
        selectFileButton.setEnabled(true);
        startTranslateButton.setEnabled(true);
        modelSpinner.setEnabled(true); // 新增：启用模型Spinner
        languageSpinner.setEnabled(true); // 新增：启用语言Spinner
    }

    @Override
    public void onSuccess(String message) {
        runOnUiThread(() -> {
            Log.d(TAG, "Translation succeeded: " + message); // 添加日志：翻译成功
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            // 增加提示：即将返回主界面
            Toast.makeText(this, "即将返回主界面", Toast.LENGTH_LONG).show();
            // 跳转到主界面的逻辑
            new Handler().postDelayed(() -> {
                enableButtons();
                Intent intent = new Intent(TranslateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, delayTimes);
        });
    }

    @Override
    public void onFailure(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "Translation failed: " + error); // 添加日志：翻译失败
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            // 增加提示：即将返回主界面
            Toast.makeText(this, "即将返回主界面", Toast.LENGTH_LONG).show();
            // 跳转到主界面的逻辑
            new Handler().postDelayed(() -> {
                enableButtons();
                Intent intent = new Intent(TranslateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, delayTimes);
        });
    }
}