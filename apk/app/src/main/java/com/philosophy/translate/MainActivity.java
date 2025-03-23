package com.philosophy.translate;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 为翻译系统按钮设置点击事件
        Button translateButton = findViewById(R.id.translate);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TranslateActivity.class);
                startActivity(intent);
            }
        });

        // 为评估系统按钮设置点击事件
        Button evaluateButton = findViewById(R.id.evaluate);
        evaluateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EvaluateActivity.class);
                startActivity(intent);
            }
        });
    }
}