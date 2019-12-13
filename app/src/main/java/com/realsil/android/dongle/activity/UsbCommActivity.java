package com.realsil.android.dongle.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.action.MessageAction;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.util.ByteUtils;
import com.realsil.android.dongle.util.TimeUtil;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class UsbCommActivity extends AppCompatActivity {



    public static final String TAG = "xp.chen";

    private ListView lv_msg_list;
    private UsbMsgListAdapter mMsgListAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_comm);
        int ret = LocalUsbConnector.getInstance().initAdapter(getApplicationContext());
        LocalUsbConnector.getInstance().setAllowOnlyHaveOutEndpoint(false);
        if (ret < 0) {
            Log.i(TAG, "usb init failed");
        }
        initView();
    }

    private void initView() {
        lv_msg_list = findViewById(R.id.lv_msg_list);
        mMsgListAdapter = new UsbMsgListAdapter(getApplicationContext(), new ArrayList<String>());
        lv_msg_list.setAdapter(mMsgListAdapter);
    }


    private void test() {
        byte[] bArr = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x0a};
        String s = ByteUtils.convertByteArr2String(bArr);
        Log.i(TAG, "convert str: "+s);
    }


    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(MessageAction.ACTION_REQUEST_USE_USB_PERMISSION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }


    private void unRegisterReceiver() {
        unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver();
    }


    @Override
    protected void onStop() {
        super.onStop();
        unRegisterReceiver();
    }


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            int authRet = msg.arg1;
            if (authRet >= 0) {
                Toast.makeText(getApplicationContext(), "Auth success", Toast.LENGTH_SHORT).show();
                if (LocalUsbConnector.getInstance().setupDevice() >= 0) {
                    Toast.makeText(getApplicationContext(), "setup success", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "setup failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Auth failed", Toast.LENGTH_SHORT).show();
            }
        }
    };



    private UsbDataReceiver mUsbDataReceiver = new UsbDataReceiver() {
        @Override
        public void onReceiveUsbData(byte[] receiveData, int dataLength) {
            String recvStr = ByteUtils.convertByteArr2String(receiveData);
            String cmdStr = TimeUtil.getSimpleTimeStr() + ": " + recvStr;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMsgListAdapter.addMsgItem(cmdStr);
                }
            });
        }
    };



    public void setupUsbAdapter(View view) {
        int searchRet = LocalUsbConnector.getInstance().searchUsbDevice();
        if (searchRet < 0) {
            return;
        }

        new Thread(){
            @Override
            public void run() {
                super.run();
                int authorizeDevice = LocalUsbConnector.getInstance().authorizeDevice();
                Message message = mHandler.obtainMessage();
                message.arg1 = authorizeDevice;
                message.sendToTarget();
            }
        }.start();
    }

    public void startReadingFromUsb(View view) {
        LocalUsbConnector.getInstance().addUsbDataReceiver(mUsbDataReceiver);
        LocalUsbConnector.getInstance().startReadingFromFromUsb();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalUsbConnector.getInstance().releaseAdapter();
    }

    private AtomicInteger mSendCommandPduInteger = new AtomicInteger();
    private AtomicInteger mSendRequestPduInteger = new AtomicInteger();


    public void sendCommandPdu(View view) {
        CommandPDU commandPDU = new CommandPDU();
        commandPDU.setSendCmdMsg("Send Command id : "+mSendCommandPduInteger.intValue());
        mSendCommandPduInteger.incrementAndGet();
        LocalUsbConnector.getInstance().writeCommandPDU(commandPDU);
    }

    public void resetAtomic(View view) {
        mSendCommandPduInteger.set(0);
        mSendRequestPduInteger.set(0);
    }
}
