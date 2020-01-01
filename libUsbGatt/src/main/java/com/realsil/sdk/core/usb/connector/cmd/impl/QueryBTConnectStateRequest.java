package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;

public class QueryBTConnectStateRequest extends BaseUsbRequest {



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
        this.request_opcode = UsbCmdOpcodeDefine.QUERY_BT_CONN_STATE;
    }

    @Override
    public void setMessageLength() {
        this.mMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE + UsbCmdParamLengthDefine.LENGTH_USB_CMD_NON_PARAM;
    }

    @Override
    public void parseResponse(byte[] responseData) {

    }

}
