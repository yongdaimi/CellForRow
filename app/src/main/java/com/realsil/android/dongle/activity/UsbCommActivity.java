package com.realsil.android.dongle.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.realsil.android.dongle.R;

public class UsbCommActivity extends AppCompatActivity {

    public static final String TAG = UsbCommActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_comm);
    }

}
