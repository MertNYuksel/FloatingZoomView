package com.mertnevzatyuksel.floatingzoomviewexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.mertnevzatyuksel.floatingzoomview.FloatingZoomView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView originalImageView = findViewById(R.id.ivImage);
        new FloatingZoomView(originalImageView);
    }
}
