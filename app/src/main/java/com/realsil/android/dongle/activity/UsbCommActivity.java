package com.realsil.android.dongle.activity;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.action.MessageAction;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.util.ByteUtils;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.att.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.OnServerTransactionChangeCallback;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class UsbCommActivity extends AppCompatActivity {



    public static final String TAG = UsbCommActivity.class.getSimpleName();

    private ListView lv_msg_list;
    private UsbMsgListAdapter mMsgListAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_comm);

        initView();

        WriteAttributeRequest writeAttributeRequest = new WriteAttributeRequest((short) 0, new byte[]{0x00, 0x01});
        writeAttributeRequest.addWriteAttributeRequestCallback(new WriteAttributeRequestCallback() {
            @Override
            public void onRequestSendSuccess() {
                super.onRequestSendSuccess();

            }

            @Override
            public void onWriteFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onWriteFailed(att_opcode, request_code, att_handler, error_code);

            }
        });


    }



    private void initUsbConnector() {
        int initRet = LocalUsbConnector.getInstance().initConnector(getApplicationContext());
        int searchRet = LocalUsbConnector.getInstance().searchUsbDevice();
        int authorizeDevice = LocalUsbConnector.getInstance().authorizeDevice();
        int setupRet = LocalUsbConnector.getInstance().setupDevice();
        int connectRet = LocalUsbConnector.getInstance().connect();
    }



    private void addOnServerTransactionChangeCallback() {
        LocalUsbConnector.getInstance().addOnServerTransactionChangeCallback(new OnServerTransactionChangeCallback() {
            @Override
            public void onReceiveNotificationMessage(byte[] notificationData) {

            }

            @Override
            public void onReceiveIndicationMessage(byte[] indicationData) {

            }

            @Override
            public void onDeviceDisconnected() {

            }

            @Override
            public void onReceiveWriteResponseTimeout() {

            }
        });
    }



    private void writeAttributeCommand(short att_handle, byte[] att_value) {
        WriteAttributeCommand writeAttributesCommand = new WriteAttributeCommand(att_handle, att_value);
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributesCommand);
    }


    private void writeAttributeRequest(short att_handle, byte[] att_value) {
        WriteAttributeRequest writeAttributesRequest = new WriteAttributeRequest(att_handle, att_value);
        writeAttributesRequest.addWriteAttributeRequestCallback(new WriteAttributeRequestCallback() {
            @Override
            public void onRequestSendSuccess() {

            }

            @Override
            public void onRequestSendFailed(int sendResult) {

            }

            @Override
            public void onWriteSuccess() {

            }

            @Override
            public void onWriteFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {

            }

            @Override
            public void onWriteTimeout() {

            }
        });
        LocalUsbConnector.getInstance().writeAttributesRequest(writeAttributesRequest);
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalUsbConnector.getInstance().releaseAdapter();
    }

    private AtomicInteger mSendCommandPduInteger = new AtomicInteger();
    private AtomicInteger mSendRequestPduInteger = new AtomicInteger();



}
