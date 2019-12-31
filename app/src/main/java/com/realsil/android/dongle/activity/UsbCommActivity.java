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
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.entity.UsbMsg;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.UsbError;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;

import java.util.ArrayList;
import java.util.Objects;

public class UsbCommActivity extends AppCompatActivity implements View.OnClickListener {


    private Button btn_setup_usb_connector;
    private Button btn_send_att_pdu;

    private RecyclerView rv_msg_list;

    private UsbMsgListAdapter mUsbMsgListAdapter;

    private static final String ERROR_MSG_INIT_USB_CONNECTOR   = "setup usb connector failed";
    private static final String ERROR_MSG_SEARCH_USB_DEVICE    = "can not found usb device";
    private static final String ERROR_MSG_AUTHORIZE_USB_DEVICE = "usb device authorization failed";
    private static final String ERROR_MSG_SETUP_USB_DEVICE     = "usb device setup failed";
    private static final String ERROR_MSG_CONNECT_USB_DEVICE   = "failed to connect usb device";

    private static final String TIPS_MSG_CONNECT_USB_DEVICE = "connected usb device successfully";
    private static final String TIPS_MSG_USB_DEVICE_ATTACHED = "usb device has attached";
    private static final String TIPS_MSG_USB_DEVICE_DETACHED = "usb device has detached";

    private static final String TAG = UsbCommActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_comm);
        initView();
    }

    private void initView() {
        rv_msg_list = findViewById(R.id.lv_msg_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_msg_list.setLayoutManager(layoutManager);
        rv_msg_list.setHasFixedSize(true);

        mUsbMsgListAdapter = new UsbMsgListAdapter(getApplicationContext(), new ArrayList<UsbMsg>());
        rv_msg_list.setAdapter(mUsbMsgListAdapter);

        btn_setup_usb_connector = findViewById(R.id.btn_setup_usb_connector);
        btn_setup_usb_connector.setOnClickListener(this);

        btn_send_att_pdu = findViewById(R.id.btn_send_att_pdu);
        btn_send_att_pdu.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_setup_usb_connector:
                setup_usb_connector();
                break;
            case R.id.btn_send_att_pdu:
                send_att_pdu_to_server();
                break;
            default:
                break;
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            UsbMsg usbMsg = (UsbMsg) msg.obj;
            mUsbMsgListAdapter.addMsgItem(usbMsg);
            mUsbMsgListAdapter.notifyItemInserted(0);
            rv_msg_list.scrollToPosition(0);
        }
    };

    private void setup_usb_connector() {
        int initRet = LocalUsbConnector.getInstance().initConnector(getApplicationContext());
        if (initRet != UsbError.CODE_NO_ERROR) {
            sendMessage(new UsbMsg(ERROR_MSG_INIT_USB_CONNECTOR, initRet));
            return;
        }

        int searchRet = LocalUsbConnector.getInstance().searchUsbDevice();
        if (searchRet != UsbError.CODE_NO_ERROR) {
            sendMessage(new UsbMsg(ERROR_MSG_SEARCH_USB_DEVICE, searchRet));
            return;
        }

        int authorizeRet = LocalUsbConnector.getInstance().authorizeDevice();
        if (authorizeRet != UsbError.CODE_NO_ERROR) {
            sendMessage(new UsbMsg(ERROR_MSG_AUTHORIZE_USB_DEVICE, authorizeRet));
        }

        LocalUsbConnector.getInstance().addOnUsbDeviceStatusChangeCallback(mOnUsbDeviceStatusChangeCallback);
    }

    private OnUsbDeviceStatusChangeCallback mOnUsbDeviceStatusChangeCallback = new OnUsbDeviceStatusChangeCallback() {
        @Override
        public void authorizeCurrentDevice(boolean authorizeResult) {
            if (authorizeResult) {
                int setupRet = LocalUsbConnector.getInstance().setupDevice();
                if (setupRet != UsbError.CODE_NO_ERROR) {
                    sendMessage(new UsbMsg(ERROR_MSG_SETUP_USB_DEVICE, setupRet));
                    return;
                }

                int connectRet = LocalUsbConnector.getInstance().connect();
                if (connectRet != UsbError.CODE_NO_ERROR) {
                    sendMessage(new UsbMsg(ERROR_MSG_CONNECT_USB_DEVICE, connectRet));
                    return;
                }

                sendMessage(new UsbMsg(TIPS_MSG_CONNECT_USB_DEVICE));
            } else {
                sendMessage(new UsbMsg(ERROR_MSG_AUTHORIZE_USB_DEVICE, UsbMsg.MSG_TYPE_ERROR));
            }
        }

        @Override
        public void onDeviceHasConnected() {

        }

        @Override
        public void onDeviceHasDisconnected() {

        }

        @Override
        public void onDeviceStatusChange(int errorCode, String detailInfo) {

        }
    };

    private void send_att_pdu_to_server() {
        WriteAttributeCommand writeAttributeCommand = new WriteAttributeCommand((short) 0x0005, new byte[]{0x01, 0x02, 0x03});
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommand);
    }

    private void sendMessage(UsbMsg usbMsg) {
        if (usbMsg != null) {
            Message msg = mHandler.obtainMessage();
            msg.obj = usbMsg;
            msg.sendToTarget();
        }
    }


    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void destroyReceiver() {
        unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                sendMessage(new UsbMsg(TIPS_MSG_USB_DEVICE_ATTACHED));
            }

            if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                sendMessage(new UsbMsg(TIPS_MSG_USB_DEVICE_DETACHED, UsbMsg.MSG_TYPE_ERROR));
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        initReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        destroyReceiver();
    }


}
