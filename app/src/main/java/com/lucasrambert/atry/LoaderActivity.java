package com.lucasrambert.atry;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class LoaderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loader_activity); // This layout contains your splash image

        new Handler().postDelayed(() -> {
            startActivity(new Intent(LoaderActivity.this, MainActivity.class));
            finish();
        }, 1000); // Delay for 1 second
    }
}
