package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.UsbConfig;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadLocalChipVersionInfoRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use this class to read the version information of the local bluetooth chip.
 */
public class ReadLocalChipVersionInfoRequest extends BaseUsbRequest {


    /**
     * Add a callback to the current request to listen the status of sending and receiving.
     *
     * @param callback Callback for status listening
     */
    public void addReadLocalChipVersionInfoRequestCallback(ReadLocalChipVersionInfoRequestCallback callback) {
        this.mBaseRequestCallback = callback;
    }

    /**
     * Get the callback that is currently used to listen the status.
     *
     * @return Callback for status listening
     */
    public ReadLocalChipVersionInfoRequestCallback getReadLocalChipVersionInfoRequestCallback() {
        return (ReadLocalChipVersionInfoRequestCallback) mBaseRequestCallback;
    }


    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.READ_LOCAL_VERSION_INFORMATION;
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
        // ReportID Note: Report ID = 4
        mSendReportID = UsbConfig.REPORT_ID_4;
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

            int hciVersion = buffer.get(8) & 0x0FF;
            int hciRevision = buffer.getShort(9) & 0x0FFFF;
            int lmpVersion = buffer.get(11) & 0x0FF;
            int manufacturerName = buffer.getShort(12) & 0x0FFFF;
            int lmpSubVersion = buffer.getShort(14) & 0x0FFFF;

            if (getReadLocalChipVersionInfoRequestCallback() != null) {
                getReadLocalChipVersionInfoRequestCallback().onReceivedVersionInformation(hciVersion, hciRevision, lmpVersion, lmpSubVersion, manufacturerName);
            }

        } else {
            if (getReadLocalChipVersionInfoRequestCallback() != null) {
                getReadLocalChipVersionInfoRequestCallback().onReceiveFailed();
            }
        }
    }

}
