package com.realsil.android.dongle.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.base.BaseActivity;

public class SplashActivity extends BaseActivity {


    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };


    private static final int REQUEST_CODE_APPLY_PERMISSION = 10001;


    @Override
    protected void setContainer() {
        setContentView(R.layout.activity_splash);
        // ScreenUtil.hideNavigationBar(this);
    }

    @Override
    protected void init() {

    }

    @Override
    protected void setListener() {

    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!checkRuntimePermission()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_APPLY_PERMISSION);
        } else {
            skip2MainActivity();
        }
    }


    private void skip2MainActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                 Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                Intent intent = new Intent(getApplicationContext(), UsbCommActivity.class);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }


    private boolean checkRuntimePermission() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), getString(R.string.app_tips_open_permission), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        skip2MainActivity();
    }


}
