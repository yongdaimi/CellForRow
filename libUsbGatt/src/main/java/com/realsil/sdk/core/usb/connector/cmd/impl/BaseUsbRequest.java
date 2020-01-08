package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.BaseRequest;
import com.realsil.sdk.core.usb.connector.UsbConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class BaseUsbRequest extends BaseRequest {


    /**
     * The request opcode sent by client. If defined in {@link com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine}
     *
     * @see com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine#QUERY_BT_CONN_STATE_REQUEST
     */
    short request_opcode;

    /**
     * The response opcode received from USB.
     */
    short response_opcode;

    /**
     * Current command status code
     */
    byte status_code = -1;

    /**
     * 0 means processing was successful
     */
    static final byte STATUS_SUCCESS = 0;

    /**
     * Use this method to create a Write Attributes Request.
     */
    @Override
    public void createRequest() {
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mSendMessageLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = UsbConfig.REPORT_ID_4;
    }

    @Override
    public void parseResponse(byte[] responseData) {
        this.mReceiveMessageLength = responseData[1] & 0x0FF;
        ByteBuffer buffer = ByteBuffer.wrap(responseData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.response_opcode = buffer.getShort(5);
        this.status_code = buffer.get(7);
    }

}
