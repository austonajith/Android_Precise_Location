package com.drum.maker.myapplicationjava;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private LocationUtils locationUtils = null;
    private Button fetchBtn = null;
    private TextView logTv = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        initLocation();
        fetchBtn = findViewById(R.id.btnFetch);
        logTv = findViewById(R.id.logTv);
        fetchBtn.setOnClickListener(view -> {
            locationUtils.scanLocation();
        });
    }

    private void initLocation() {
        locationUtils = new LocationUtils(this, new LocationUtils.LocationListener() {
            @Override
            public void onLocationFetchSuccess(@NonNull Location result, boolean isPreciseLocation, @Nullable String api) {
                String data = "lat = " + result.getLatitude() + " long = " + result.getLongitude() + " Precise = " + isPreciseLocation + " | api = " + api;
                Log.d(TAG, data);
                logTv.append("\n" + data);
            }

            @Override
            public void onLocationFetchFailed(@NonNull String error) {
                Log.d(TAG, error);
                logTv.append("\n" + error);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationUtils.onActivityResult(requestCode, resultCode, data);
    }

}