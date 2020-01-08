package com.realsil.android.dongle.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.realsil.android.dongle.DongleApplication;
import com.realsil.android.dongle.R;

public abstract class BaseActivity extends AppCompatActivity {


    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = DongleApplication.getInstance().getContext();
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

    Toast mToast = null;

    protected final void showToast(final String debugInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = new Toast(mContext);
                    mToast.setView(View.inflate(mContext, R.layout.widget_toast, null));
                    mToast.setText(debugInfo);
                    mToast.setDuration(Toast.LENGTH_LONG);
                } else {
                    mToast.setText(debugInfo);
                }
                mToast.show();
            }
        });
    }


    protected final void showToast(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = new Toast(mContext);
                    mToast.setView(View.inflate(mContext, R.layout.widget_toast, null));
                    mToast.setText(resId);
                    mToast.setDuration(Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(resId);
                }
                mToast.show();
            }
        });
    }


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
