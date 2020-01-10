package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.UsbGatt;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QueryBTConnectStateRequest extends BaseUsbRequest {

    /**
     * If bt is connected, the connect status is 1.
     */
    private static final byte BT_HAS_CONNECTED = 1;

    /**
     * If bt is disConnected, the connect status is 0.
     */
    private static final byte BT_HAS_DISCONNECTED = 0;

    /**
     * Add a callback interface to listen the connect status of bluetooth.
     *
     * @param queryBTConnectStateRequestCallback A callback is used to listen the connect status of bluetooth.
     */
    public void addQueryBTConnectStateRequestCallback(QueryBTConnectStateRequestCallback queryBTConnectStateRequestCallback) {
        this.mBaseRequestCallback = queryBTConnectStateRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link QueryBTConnectStateRequest}.
     *
     * @return A Callback currently for listening to {@link QueryBTConnectStateRequest}.
     */
    public QueryBTConnectStateRequestCallback getQueryBTConnectStateRequestCallback() {
        return (QueryBTConnectStateRequestCallback) mBaseRequestCallback;
    }


    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.QUERY_BT_CONN_STATE_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE_FIELD + UsbCmdParamLengthDefine.LENGTH_PARAMETER_TOTAL_LEN_FIELD;
    }

    @Override
    public void createRequest() {
        super.createRequest();

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mSendMessageLength);

        /// Put USB PDU
        // Usb opcode
        byteBuffer.putShort(2, request_opcode);
    }

    @Override
    public void parseResponse(byte[] responseData) {
        super.parseResponse(responseData);
        if (response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            byte connectStatus = responseData[8];
            if (getQueryBTConnectStateRequestCallback() != null) {
                getQueryBTConnectStateRequestCallback().onReceiveConnectState(STATUS_SUCCESS,
                        connectStatus == BT_HAS_CONNECTED ? UsbGatt.STATE_CONNECTED : UsbGatt.STATE_DISCONNECTED);
            }
        } else {
            if (getQueryBTConnectStateRequestCallback() != null) {
                getQueryBTConnectStateRequestCallback().onReceiveConnectState(status_code, BT_HAS_DISCONNECTED);
            }
        }
    }

}
