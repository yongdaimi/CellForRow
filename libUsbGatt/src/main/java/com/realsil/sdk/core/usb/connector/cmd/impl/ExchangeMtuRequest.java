package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.ExchangeMtuRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ExchangeMtuRequest extends BaseUsbRequest {


    /**
     * Add a callback interface to listen the exchange Mtu request.
     *
     * @param exchangeMtuRequestCallback A callback is used to listen the exchange mtu request.
     */
    public void addExchangeMtuRequestCallback(ExchangeMtuRequestCallback exchangeMtuRequestCallback) {
        this.mBaseRequestCallback = (BaseRequestCallback) exchangeMtuRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link ExchangeMtuRequest}.
     *
     * @return A Callback currently for listening to {@link ExchangeMtuRequest}.
     */
    public ExchangeMtuRequestCallback getExchangeMtuRequestCallback() {
        return (ExchangeMtuRequestCallback) mBaseRequestCallback;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.EXCHANGE_MTU_REQUEST;
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
        byteBuffer.put(mSendReportID);
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
            ByteBuffer buffer = ByteBuffer.wrap(responseData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            short serverMtuSize = buffer.getShort(8);

            if (getExchangeMtuRequestCallback() != null) {
                getExchangeMtuRequestCallback().onReceiveServerRxMtu(serverMtuSize & 0xFFFFFFFF);
            }
        } else {
            if (getExchangeMtuRequestCallback() != null) {
                getExchangeMtuRequestCallback().onReceiveFailed();
            }
        }
    }

}
