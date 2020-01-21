package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.UsbConfig;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadRomVersionCommandCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Call an instance of this class to get the version of the rom
 */
public class ReadRomVersionCommand extends BaseUsbRequest {


    /**
     * Add a callback to the current command to listen the status of sending and receiving.
     *
     * @param callback Callback for status listening
     */
    public void addReadRomVersionCommandCallback(ReadRomVersionCommandCallback callback) {
        this.mBaseRequestCallback = callback;
    }

    /**
     * Get the callback that is currently used to listen the status.
     *
     * @return Callback for status listening
     */
    public ReadRomVersionCommandCallback getReadRomVersionCommandCallback() {
        return (ReadRomVersionCommandCallback) mBaseRequestCallback;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.VENDOR_READ_ROM_VERSION;
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
        // ReportID Note: Report ID = 5 in Download Patch in normal mode
        mSendReportID = UsbConfig.REPORT_ID_5;
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
        if (mReceiveReportID == mSendReportID && response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            ByteBuffer buffer = ByteBuffer.wrap(responseData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            // If you want to compare firmware's chip id in the future, you need to add 1
            // to this chip_id, Note: the chip id here is a unsigned char type
            int chip_id = buffer.get(8) & 0x0FF;
            if (getReadRomVersionCommandCallback() != null) {
                getReadRomVersionCommandCallback().onReadRomVersionSuccess(chip_id);
            }
        } else {
            if (getReadRomVersionCommandCallback() != null) {
                getReadRomVersionCommandCallback().onReadRomVersionFail();
            }
        }
    }

}
