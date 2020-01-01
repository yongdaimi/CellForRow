package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.BaseRequest;
import com.realsil.sdk.core.usb.connector.UsbConfig;

public abstract class BaseUsbRequest extends BaseRequest {


    /**
     * The request opcode sent by client. If defined in {@link com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine}
     *
     * @see com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine#QUERY_BT_CONN_STATE
     */
    short request_opcode;


    /**
     * Use this method to create a Write Attributes Request.
     */
    @Override
    public void createRequest() {
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mMessageLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = UsbConfig.REPORT_ID_4;
    }


}
