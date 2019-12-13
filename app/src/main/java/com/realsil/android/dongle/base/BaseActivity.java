package com.realsil.android.dongle.base;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.realsil.android.dongle.R;

public abstract class BaseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContainer();
        init();
        setListener();
    }

    /**
     * Init UI content
     */
    protected abstract void setContainer();

    /**
     * Init view
     */
    protected abstract void init();

    /**
     * Init UI listener
     */
    protected abstract void setListener();

    protected void pushAnimActivity(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
    }

    /*@Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.anim_back_enter, R.anim.anim_back_exit);
    }*/
}
