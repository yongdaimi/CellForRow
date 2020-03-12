package com.realsil.android.dongle.fragment;

import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.base.BaseFragment;
import com.realsil.android.dongle.helper.UsbAudioHelper;
import com.realsil.android.dongle.util.FileUtil;

import java.io.File;

/**
 * @author xp.chen
 */
public class UsbAudioFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = "xp.chen";

    private String mRecordFilePath;
    private String mPlayFilePath;

    /* Record Sub */
    private Button btn_start_record;
    private Button btn_stop_record;

    /* Play Sub */
    private Button btn_start_play;
    private Button btn_pause_play;
    private Button btn_stop_play;

    @Override
    protected void setContainer() {
        setContentView(R.layout.fragment_usb_audio);
    }

    @Override
    protected void init() {
        btn_start_record = findViewById(R.id.btn_start_record);
        btn_stop_record = findViewById(R.id.btn_stop_record);

        btn_start_play = findViewById(R.id.btn_start_play);
        btn_pause_play = findViewById(R.id.btn_pause_play);
        btn_stop_play = findViewById(R.id.btn_stop_play);
    }

    @Override
    protected void setListener() {
        btn_start_record.setOnClickListener(this);
        btn_stop_record.setOnClickListener(this);

        btn_start_play.setOnClickListener(this);
        btn_pause_play.setOnClickListener(this);
        btn_stop_play.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_record:
                startRecord();
                break;
            case R.id.btn_stop_record:
                stopRecord();
                break;
            case R.id.btn_start_play:
                startPlay();
                break;
            case R.id.btn_pause_play:
                pausePlay();
                break;
            case R.id.btn_stop_play:
                stopPlay();
                break;
            default:
                break;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (TextUtils.isEmpty(mRecordFilePath)) {
            File musicFilesDir = mContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (musicFilesDir != null && musicFilesDir.exists()) {
                mRecordFilePath = musicFilesDir.getAbsolutePath() + File.separator + "record.pcm";
            }
        }

        if (TextUtils.isEmpty(mPlayFilePath)) {
            mPlayFilePath = "/sdcard/test.pcm";
        }
    }


    private void disableAllButton() {
        btn_start_record.setEnabled(false);
        btn_stop_record.setEnabled(false);

        btn_start_play.setEnabled(false);
        btn_pause_play.setEnabled(false);
        btn_stop_play.setEnabled(false);
    }

    private void enableAllButton() {
        btn_start_record.setEnabled(true);
        btn_stop_record.setEnabled(true);

        btn_start_play.setEnabled(true);
        btn_pause_play.setEnabled(true);
        btn_stop_play.setEnabled(true);
    }

    private void startRecord() {
        disableAllButton();
        btn_stop_record.setEnabled(true);
        UsbAudioHelper.native_start_record(mRecordFilePath);
    }

    private void stopRecord() {
        enableAllButton();
        UsbAudioHelper.native_stop_record();
        FileUtil.notifySystemToScan(mContext, mRecordFilePath);
    }

    private void startPlay() {
        disableAllButton();
        btn_pause_play.setEnabled(true);
        btn_stop_play.setEnabled(true);
        UsbAudioHelper.native_start_play(mPlayFilePath);
    }

    private void stopPlay() {
        enableAllButton();
        UsbAudioHelper.native_stop_play();
    }

    private void pausePlay() {
        disableAllButton();
        btn_start_play.setEnabled(true);
        btn_stop_play.setEnabled(true);
        UsbAudioHelper.native_pause_play();
    }

    public static UsbAudioFragment newInstance() {
        UsbAudioFragment mFragment = new UsbAudioFragment();
        Bundle args = new Bundle();
        mFragment.setArguments(args);
        return mFragment;
    }

}
