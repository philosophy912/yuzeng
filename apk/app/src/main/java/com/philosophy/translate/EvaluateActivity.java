package com.philosophy.translate;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import java.io.File;

public class EvaluateActivity extends AppCompatActivity {

    private static final String TAG = "EvaluateActivity";
    private FileSelectorHelper originFileSelectorHelper;
    private FileSelectorHelper translateFileSelectorHelper;

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

        Button startTranslateButton = findViewById(R.id.startEvaluationButton);

        startTranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取选中的文件、模型和语言
                // 这里表示选择的
                File originFile = originFileSelectorHelper.getSelectFile();
                File translateFile = translateFileSelectorHelper.getSelectFile();

                // 调用API进行翻译
                ApiHandler.evaluate(EvaluateActivity.this, originFile, translateFile);
            }
        });

    }
}
