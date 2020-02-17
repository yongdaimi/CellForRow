package com.realsil.android.dongle.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.base.BaseFragment;

/**
 * @author xp.chen
 */
public class UsbAudioFragment extends BaseFragment implements View.OnClickListener {



    static {
        System.loadLibrary("native-lib");
    }

    private Button btn_start_play;

    @Override
    protected void setContainer() {
        setContentView(R.layout.fragment_usb_audio);
    }

    @Override
    protected void init() {
        btn_start_play = findViewById(R.id.btn_start_play);
    }

    @Override
    protected void setListener() {
        btn_start_play.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_play:
                startPlay();
                break;
            default:
                break;
        }
    }


    private void startPlay() {
        native_start_play();
    }

    private native void native_start_play();


    public static UsbAudioFragment newInstance() {
        UsbAudioFragment mFragment = new UsbAudioFragment();
        Bundle args = new Bundle();
        mFragment.setArguments(args);
        return mFragment;
    }


}
