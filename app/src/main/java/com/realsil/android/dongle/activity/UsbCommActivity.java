package com.realsil.android.dongle.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.entity.UsbMsg;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.UsbError;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.QueryBTConnectStateRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.ReadDongleConfigRequest;

import java.util.ArrayList;
import java.util.Objects;

public class UsbCommActivity extends AppCompatActivity implements View.OnClickListener {


    private Button btn_send_att_pdu;
    private Button btn_setup_usb_connector;
    private Button btn_query_bt_conn_state;
    private Button btn_read_dongle_config;

    private RecyclerView rv_msg_list;

    private UsbMsgListAdapter mUsbMsgListAdapter;

    private static final String ERROR_MSG_INIT_USB_CONNECTOR   = "setup usb connector failed";
    private static final String ERROR_MSG_SEARCH_USB_DEVICE    = "can not found usb device";
    private static final String ERROR_MSG_AUTHORIZE_USB_DEVICE = "usb device authorization failed";
    private static final String ERROR_MSG_SETUP_USB_DEVICE     = "usb device setup failed";
    private static final String ERROR_MSG_CONNECT_USB_DEVICE   = "failed to connect usb device";

    private static final String TIPS_MSG_CONNECT_USB_DEVICE  = "connected usb device successfully";
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

        btn_query_bt_conn_state = findViewById(R.id.btn_query_bt_conn_state);
        btn_query_bt_conn_state.setOnClickListener(this);

        btn_read_dongle_config = findViewById(R.id.btn_read_dongle_config);
        btn_read_dongle_config.setOnClickListener(this);
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
            case R.id.btn_query_bt_conn_state:
                query_bt_conn_state();
                break;
            case R.id.btn_read_dongle_config:
                read_dongle_config_state();
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
        public void authorizeCurrentDevice(UsbDevice usbDevice, boolean authorizeResult) {
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


    };

    private void send_att_pdu_to_server() {
        // WriteAttributeCommand writeAttributeCommand = new WriteAttributeCommand((short) 0x0005, new byte[]{0x01, 0x02, 0x03});
        // LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommand);
        WriteAttributeRequest writeAttributeRequest = new WriteAttributeRequest((short) 0x0005, new byte[]{0x01, 0x02, 0x03});
        writeAttributeRequest.addWriteAttributeRequestCallback(new WriteAttributeRequestCallback() {
            @Override
            public void onWriteSuccess() {
                super.onWriteSuccess();
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                Toast.makeText(getApplicationContext(), "send failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onReceiveFailed(att_opcode, request_code, att_handler, error_code);
                Toast.makeText(getApplicationContext(), "receive failed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "receive timeout", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        LocalUsbConnector.getInstance().sendRequest(writeAttributeRequest);
    }


    private void query_bt_conn_state() {
        QueryBTConnectStateRequest queryBTConnectStateRequest = new QueryBTConnectStateRequest();
        queryBTConnectStateRequest.addQueryBTConnectStateRequestCallback(new QueryBTConnectStateRequestCallback() {
            @Override
            public void onReceiveConnectState(int statusCode, int connectState) {
                super.onReceiveConnectState(statusCode, connectState);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "statusCode: "+statusCode+", connectState: "+connectState, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        LocalUsbConnector.getInstance().sendRequest(queryBTConnectStateRequest);
    }

    private void read_dongle_config_state() {
        ReadDongleConfigRequest readDongleConfigRequest = new ReadDongleConfigRequest();
        LocalUsbConnector.getInstance().sendRequest(readDongleConfigRequest);
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
