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

public class EvaluateActivity extends AppCompatActivity implements ApiHandler.ApiHandlerCallback {

    private static final String TAG = "EvaluateActivity";
    private FileSelectorHelper originFileSelectorHelper;
    private FileSelectorHelper translateFileSelectorHelper;
    private Spinner originSpinner;
    private Spinner targetSpinner;
    private ProgressBar progressBar;
    private static final Integer delayTimes = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evaluate);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EvaluateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        Button selectFileButton = findViewById(R.id.selectOriginFileButton);
        TextView selectedFileNameTextView = findViewById(R.id.selectedFileName);

        originFileSelectorHelper = new FileSelectorHelper(EvaluateActivity.this, selectedFileNameTextView);
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                originFileSelectorHelper.showFileChooser();
            }
        });

        Button selectTranslateFileButton = findViewById(R.id.selectTranslateFileButton);
        TextView selectedTranslateFileNameTextView = findViewById(R.id.selectedTranslateFileName);

        translateFileSelectorHelper = new FileSelectorHelper(EvaluateActivity.this, selectedTranslateFileNameTextView);
        selectTranslateFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateFileSelectorHelper.showFileChooser();
            }
        });

        originSpinner = findViewById(R.id.originSpinner);
        targetSpinner = findViewById(R.id.targetSpinner);

        Button startTranslateButton = findViewById(R.id.startEvaluationButton);
        progressBar = findViewById(R.id.progressBar); // 假设进度条在布局文件中已经定义

        startTranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取选中的文件、模型和语言
                File originFile = originFileSelectorHelper.getSelectFile();
                File translateFile = translateFileSelectorHelper.getSelectFile();
                String origin = originSpinner.getSelectedItem().toString();
                String target = targetSpinner.getSelectedItem().toString();

                // 禁用所有按钮
                disableButtons();

                // 显示进度条
                progressBar.setVisibility(View.VISIBLE);
                // 调用API进行翻译
                ApiHandler.evaluate(EvaluateActivity.this, originFile, translateFile, origin, target);

            }
        });
    }

    private void disableButtons() {
        Log.d(TAG, "Disabling all buttons...");
        Button backButton = findViewById(R.id.backButton);
        Button selectFileButton = findViewById(R.id.selectOriginFileButton);
        Button selectTranslateFileButton = findViewById(R.id.selectTranslateFileButton);
        Button startTranslateButton = findViewById(R.id.startEvaluationButton);

        backButton.setEnabled(false);
        selectFileButton.setEnabled(false);
        selectTranslateFileButton.setEnabled(false);
        startTranslateButton.setEnabled(false);
        originSpinner.setEnabled(false);
        targetSpinner.setEnabled(false);
        Log.d(TAG, "All buttons disabled.");
    }

    private void enableButtons() {
        Log.d(TAG, "Enabling all buttons...");
        Button backButton = findViewById(R.id.backButton);
        Button selectFileButton = findViewById(R.id.selectOriginFileButton);
        Button selectTranslateFileButton = findViewById(R.id.selectTranslateFileButton);
        Button startTranslateButton = findViewById(R.id.startEvaluationButton);

        backButton.setEnabled(true);
        selectFileButton.setEnabled(true);
        selectTranslateFileButton.setEnabled(true);
        startTranslateButton.setEnabled(true);
        originSpinner.setEnabled(true);
        targetSpinner.setEnabled(true);
        Log.d(TAG, "All buttons enabled.");
    }

    @Override
    public void onSuccess(String message) {
        runOnUiThread(() -> {
            Log.d(TAG, "Evaluation succeeded: " + message); // 添加日志：评估成功
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            // 增加提示：即将返回主界面
            Toast.makeText(this, "即将返回主界面", Toast.LENGTH_LONG).show();
            // 跳转到主界面的逻辑
            new Handler().postDelayed(() -> {
                enableButtons();
                Intent intent = new Intent(EvaluateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, delayTimes);
        });
    }

    @Override
    public void onFailure(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "Evaluation failed: " + error); // 添加日志：评估失败
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            // 增加提示：即将返回主界面
            Toast.makeText(this, "即将返回主界面", Toast.LENGTH_LONG).show();
            // 跳转到主界面的逻辑
            new Handler().postDelayed(() -> {
                enableButtons();
                Intent intent = new Intent(EvaluateActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }, delayTimes);
        });
    }
}