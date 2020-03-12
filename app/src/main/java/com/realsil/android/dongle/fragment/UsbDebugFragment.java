package com.realsil.android.dongle.fragment;


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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.adapter.UsbMsgListAdapter;
import com.realsil.android.dongle.base.BaseFragment;
import com.realsil.android.dongle.entity.UsbMsg;
import com.realsil.sdk.core.usb.UsbGattCharacteristic;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.UsbEndpointTypeDefine;
import com.realsil.sdk.core.usb.connector.UsbError;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeCommandCallback;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommandTest1;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommandTest2;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommandTest3;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadDongleConfigRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.QueryBTConnectStateRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.ReadDongleConfigRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class UsbDebugFragment extends BaseFragment implements View.OnClickListener {


    private Button btn_send_att_pdu;
    private Button btn_setup_usb_connector;
    private Button btn_query_bt_conn_state;
    private Button btn_read_dongle_config;
    private Button btn_write_attribute;

    private Button btn_write_attribute_2;
    private Button btn_write_attribute_4;
    private Button btn_write_attribute_5;
    private Button btn_write_attribute_8;

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

    private static final String TAG = "xp.chen";

    private ImageButton ib_usb_settings;
    private FrameLayout fl_hud_container;


    /********************** Test Code ************************/
    private Button btn_start_send_test_1;
    private Button btn_stop_send_test_1;

    private Button btn_start_send_test_2;
    private Button btn_stop_send_test_2;

    private Button btn_start_send_test_3;

    /********************** Test Code ************************/


    @Override
    protected void setContainer() {
        setContentView(R.layout.fragment_usb_debug);
    }

    @Override
    protected void init() {
        rv_msg_list = findViewById(R.id.lv_msg_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv_msg_list.setLayoutManager(layoutManager);
        rv_msg_list.setHasFixedSize(true);

        mUsbMsgListAdapter = new UsbMsgListAdapter(mContext, new ArrayList<UsbMsg>());
        rv_msg_list.setAdapter(mUsbMsgListAdapter);

        btn_setup_usb_connector = findViewById(R.id.btn_setup_usb_connector);
        btn_send_att_pdu = findViewById(R.id.btn_send_att_pdu);

        btn_query_bt_conn_state = findViewById(R.id.btn_query_bt_conn_state);
        btn_read_dongle_config = findViewById(R.id.btn_read_dongle_config);

        btn_write_attribute = findViewById(R.id.btn_write_attribute);
        btn_write_attribute_2 = findViewById(R.id.btn_write_attribute_2);
        btn_write_attribute_4 = findViewById(R.id.btn_write_attribute_4);
        btn_write_attribute_5 = findViewById(R.id.btn_write_attribute_5);
        btn_write_attribute_8 = findViewById(R.id.btn_write_attribute_8);

        ib_usb_settings = findViewById(R.id.ib_usb_settings);
        fl_hud_container = findViewById(R.id.fl_hud_container);

        /********************** Test Code ************************/
        btn_start_send_test_1 = findViewById(R.id.btn_start_send_test_1);
        btn_stop_send_test_1 = findViewById(R.id.btn_stop_send_test_1);

        btn_start_send_test_2 = findViewById(R.id.btn_start_send_test_2);
        btn_stop_send_test_2 = findViewById(R.id.btn_stop_send_test_2);

        btn_start_send_test_3 = findViewById(R.id.btn_start_send_test_3);
        /********************** Test Code ************************/
    }

    @Override
    protected void setListener() {
        btn_setup_usb_connector.setOnClickListener(this);
        btn_send_att_pdu.setOnClickListener(this);
        btn_query_bt_conn_state.setOnClickListener(this);
        btn_read_dongle_config.setOnClickListener(this);

        btn_write_attribute.setOnClickListener(this);
        btn_write_attribute_2.setOnClickListener(this);
        btn_write_attribute_4.setOnClickListener(this);
        btn_write_attribute_5.setOnClickListener(this);
        btn_write_attribute_8.setOnClickListener(this);

        ib_usb_settings.setOnClickListener(this);

        /********************** Test Code ************************/
        btn_start_send_test_1.setOnClickListener(this);
        btn_stop_send_test_1.setOnClickListener(this);

        btn_start_send_test_2.setOnClickListener(this);
        btn_stop_send_test_2.setOnClickListener(this);

        btn_start_send_test_3.setOnClickListener(this);
        /********************** Test Code ************************/

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
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
            case R.id.btn_write_attribute:
                writeAttributeRequest((byte) 0x10);
                break;
            case R.id.btn_write_attribute_2:
                writeAttributeRequest((byte) 0x02);
                break;
            case R.id.btn_write_attribute_4:
                writeAttributeRequest((byte) 0x04);
                break;
            case R.id.btn_write_attribute_5:
                writeAttributeRequest((byte) 0x05);
                break;
            case R.id.btn_write_attribute_8:
                writeAttributeRequest((byte) 0x08);
                break;
            /********************** Test Code ************************/
            case R.id.btn_start_send_test_1:
                startSendTestCmd1();
                break;
            case R.id.btn_stop_send_test_1:
                stopSendTestCmd1();
                break;
            case R.id.btn_start_send_test_2:
                startSendTestCmd2();
                break;
            case R.id.btn_stop_send_test_2:
                stopSendTestCmd2();
                break;
            case R.id.btn_start_send_test_3:
                startSendTestCmd3();
                break;
            /********************** Test Code ************************/
            case R.id.ib_usb_settings:
                showUsbDebugSettingsUI();
                break;
            default:
                break;
        }
    }


    /********************** Test Code ************************/
    private void startSendTestCmd1() {
        mStartSendTestData1 = true;
        WriteAttributeCommandTest1 writeAttributeCommandTest1 = new WriteAttributeCommandTest1();
        writeAttributeCommandTest1.addWriteAttributeCommandCallback(new WriteAttributeCommandCallback() {
            @Override
            public void onSendSuccess() {
                super.onSendSuccess();
                mTestHandler.obtainMessage(1).sendToTarget();
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                sendMessage(new UsbMsg("send Test Data 1 failed", -1));
            }
        });
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommandTest1);
    }

    private volatile boolean mStartSendTestData1 = true;

    private void stopSendTestCmd1() {
        mStartSendTestData1 = false;
    }


    private void startSendTestCmd2() {
        WriteAttributeCommandTest2 writeAttributeCommandTest2 = new WriteAttributeCommandTest2();
        writeAttributeCommandTest2.addWriteAttributeCommandCallback(new WriteAttributeCommandCallback() {
            @Override
            public void onSendSuccess() {
                super.onSendSuccess();

            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                sendMessage(new UsbMsg("start send Test Data 2 failed", -1));
            }
        });
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommandTest2);
    }

    private void stopSendTestCmd2() {
        WriteAttributeCommandTest3 writeAttributeCommandTest3 = new WriteAttributeCommandTest3();
        writeAttributeCommandTest3.addWriteAttributeCommandCallback(new WriteAttributeCommandCallback() {
            @Override
            public void onSendSuccess() {
                super.onSendSuccess();

            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                sendMessage(new UsbMsg("stop send Test Data 2 failed", -1));
            }
        });
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommandTest3);
    }


    private Handler mTestHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                if (mStartSendTestData1) {
                    startSendTestCmd1();
                }
            }
        }
    };

    private void startSendTestCmd3() {
        WriteAttributeCommandTest3 writeAttributeCommandTest3 = new WriteAttributeCommandTest3();
        writeAttributeCommandTest3.addWriteAttributeCommandCallback(new WriteAttributeCommandCallback() {
            @Override
            public void onSendSuccess() {
                super.onSendSuccess();

            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                sendMessage(new UsbMsg("start send Test Data 3 failed", -1));
            }
        });
        LocalUsbConnector.getInstance().writeAttributesCommand(writeAttributeCommandTest3);
    }

    /********************** Test Code ************************/


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
        int initRet = LocalUsbConnector.getInstance().initConnector(mContext);
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

        @Override
        public void onDeviceStatusChange(int errorCode, String detailInfo) {
            sendMessage(new UsbMsg(detailInfo, -1));
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
                showToast("write success");
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                showToast("send failed");
            }

            @Override
            public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onReceiveFailed(att_opcode, request_code, att_handler, error_code);
                showToast("receive failed");
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                showToast("receive timeout");
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
                showToast("statusCode: " + statusCode + ", connectState: " + connectState);
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                showToast("query bt conn state timeout");
            }
        });
        LocalUsbConnector.getInstance().sendRequest(queryBTConnectStateRequest);
    }

    private void read_dongle_config_state() {
        ReadDongleConfigRequest readDongleConfigRequest = new ReadDongleConfigRequest();
        readDongleConfigRequest.addReadDongleConfigRequestCallback(new ReadDongleConfigRequestCallback() {
            @Override
            public void onReadOtaCharacteristicList(List<UsbGattCharacteristic> list) {
                super.onReadOtaCharacteristicList(list);
                showToast("read dongle config success, " + "Characteristic size is: " + list.size());
            }

            @Override
            public void onReadFailed() {
                super.onReadFailed();
                showToast("read dongle config failed");
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                showToast("read dongle config timeout");
            }
        });

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
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private void destroyReceiver() {
        mContext.unregisterReceiver(mBroadcastReceiver);
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


    private void writeAttributeRequest(byte reportId) {
        WriteAttributeRequest writeAttributeRequest = new WriteAttributeRequest((short) 0x5e, new byte[]{0x09}, reportId);
        writeAttributeRequest.addWriteAttributeRequestCallback(new WriteAttributeRequestCallback() {
            @Override
            public void onWriteSuccess() {
                super.onWriteSuccess();
                showToast("onWriteSuccess");
            }

            @Override
            public void onSendSuccess() {
                super.onSendSuccess();
                showToast("onSendSuccess");
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                showToast("onSendFailed");
            }

            @Override
            public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onReceiveFailed(att_opcode, request_code, att_handler, error_code);
                showToast("onReceiveFailed");
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                showToast("onReceiveTimeout");
            }
        });
        LocalUsbConnector.getInstance().sendRequest(writeAttributeRequest);
    }

    public static UsbDebugFragment newInstance() {
        UsbDebugFragment mFragment = new UsbDebugFragment();
        Bundle args = new Bundle();
        mFragment.setArguments(args);
        return mFragment;
    }


    /****************************** Endpoint Switch ************************************/

    private void clearHudSelectedStatus(LinearLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            if (view instanceof TextView) {
                view.setBackgroundResource(R.drawable.shape_unselect_box_bg);
            }
        }
    }

    private void showUsbDebugSettingsUI() {
        if (LocalUsbConnector.getInstance().getUsbConnectState() != LocalUsbConnector.STATE_USB_CONNECTED) {
            showToast("please connect usb at first");
            return;
        }

        if (fl_hud_container.getChildCount() > 1) {
            fl_hud_container.removeViewAt(1);
        } else {
            View settingsView = LayoutInflater.from(mContext).inflate(R.layout.include_usb_data_debug_settings, null);
            fl_hud_container.addView(settingsView);
            settingsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fl_hud_container.removeViewAt(1);
                }
            });


            LinearLayout ll_send_option_container = settingsView.findViewById(R.id.ll_send_option_container);
            LinearLayout ll_receive_option_container = settingsView.findViewById(R.id.ll_receive_option_container);


            TextView tv_send_on_bulk_out = settingsView.findViewById(R.id.tv_send_on_bulk_out);
            tv_send_on_bulk_out.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setSendUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_BULK_OUT);
                    if (ret) {
                        clearHudSelectedStatus(ll_send_option_container);
                        tv_send_on_bulk_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found bulk out endpoint");
                    }
                }
            });

            TextView tv_send_on_interrupt_out = settingsView.findViewById(R.id.tv_send_on_interrupt_out);
            tv_send_on_interrupt_out.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setSendUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_INTERRUPT_OUT);
                    if (ret) {
                        clearHudSelectedStatus(ll_send_option_container);
                        tv_send_on_interrupt_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found interrupt out endpoint");
                    }
                }
            });

            TextView tv_send_on_control_out = settingsView.findViewById(R.id.tv_send_on_control_out);
            tv_send_on_control_out.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setSendUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_CONTROL_OUT);
                    if (ret) {
                        clearHudSelectedStatus(ll_send_option_container);
                        tv_send_on_control_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found control out endpoint");
                    }
                }
            });

            TextView tv_receive_on_bulk_in = settingsView.findViewById(R.id.tv_receive_on_bulk_in);
            tv_receive_on_bulk_in.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setReceiveUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_BULK_IN);
                    if (ret) {
                        clearHudSelectedStatus(ll_receive_option_container);
                        tv_receive_on_bulk_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found bulk in endpoint");
                    }
                }
            });

            TextView tv_receive_on_interrupt_in = settingsView.findViewById(R.id.tv_receive_on_interrupt_in);
            tv_receive_on_interrupt_in.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setReceiveUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_INTERRUPT_IN);
                    if (ret) {
                        clearHudSelectedStatus(ll_receive_option_container);
                        tv_receive_on_interrupt_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found interrupt in endpoint");
                    }
                }
            });


            TextView tv_receive_on_control_in = settingsView.findViewById(R.id.tv_receive_on_control_in);
            tv_receive_on_control_in.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean ret = LocalUsbConnector.getInstance().setReceiveUsbEndpointType(UsbEndpointTypeDefine.USB_ENDPOINT_CONTROL_IN);
                    if (ret) {
                        clearHudSelectedStatus(ll_receive_option_container);
                        tv_receive_on_control_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                    } else {
                        showToast("set failed, can not found control in endpoint");
                    }
                }
            });

            int sendUsbEndpointType = LocalUsbConnector.getInstance().getSendUsbEndpointType();
            if (sendUsbEndpointType != UsbEndpointTypeDefine.USB_ENDPOINT_NONE) {
                if (sendUsbEndpointType == UsbEndpointTypeDefine.USB_ENDPOINT_BULK_OUT) {
                    tv_send_on_bulk_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                } else if (sendUsbEndpointType == UsbEndpointTypeDefine.USB_ENDPOINT_INTERRUPT_OUT){
                    tv_send_on_interrupt_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                } else {
                    tv_send_on_control_out.setBackgroundResource(R.drawable.shape_select_box_bg);
                }
            }

            int receiveUsbEndpointType = LocalUsbConnector.getInstance().getReceiveUsbEndpointType();
            if (receiveUsbEndpointType != UsbEndpointTypeDefine.USB_ENDPOINT_NONE) {
                if (receiveUsbEndpointType == UsbEndpointTypeDefine.USB_ENDPOINT_BULK_IN) {
                    tv_receive_on_bulk_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                } else if (receiveUsbEndpointType == UsbEndpointTypeDefine.USB_ENDPOINT_INTERRUPT_IN) {
                    tv_receive_on_interrupt_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                } else {
                    tv_receive_on_control_in.setBackgroundResource(R.drawable.shape_select_box_bg);
                }
            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        destroyReceiver();
    }

}
