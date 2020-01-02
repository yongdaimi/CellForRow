package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author xp.chen
 */
public class ReadDongleConfigRequest extends BaseUsbRequest {

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.READ_USB_DONGLE_CONFIG_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE + UsbCmdParamLengthDefine.LENGTH_USB_CMD_NON_PARAM;
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

    }
}
