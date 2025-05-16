package com.step.tcd_rpkb;

import android.os.Bundle;
import android.widget.TextView;

import com.step.tcd_rpkb.base.BaseFullscreenActivity;

public class Peredacha_LVC extends BaseFullscreenActivity {
    private TextView test;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peredacha_lvc);
        test = findViewById(R.id.peredacha_textView);

        // Получаем JSON-строку из Intent
        String jsonData = getIntent().getStringExtra("testOutput");

        if (jsonData != null) {
            test.setText(jsonData);  // Выводим JSON в TextView
        } else {
            test.setText("Ошибка: данных нет!");
        }
    }
}