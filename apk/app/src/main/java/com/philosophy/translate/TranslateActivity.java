package com.philosophy.translate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class TranslateActivity extends AppCompatActivity {

    private static final String TAG = "TranslateActivity";
    private FileSelectorHelper translateFileSelectorHelper;
    private Spinner modelSpinner;
    private Spinner languageSpinner;
    private TextView selectedFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);

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
                // 这里表示选择的
                File file = translateFileSelectorHelper.getSelectFile();
                String model = modelSpinner.getSelectedItem().toString();
                String language = languageSpinner.getSelectedItem().toString();

                // 调用API进行翻译
                ApiHandler.translate(TranslateActivity.this, file, model, language);
            }
        });
    }
}