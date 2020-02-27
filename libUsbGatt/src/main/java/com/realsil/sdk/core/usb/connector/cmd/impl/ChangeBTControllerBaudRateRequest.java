package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.UsbConfig;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.ChangeBTControllerBaudRateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadLocalChipVersionInfoRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use this class to change the baud rate value of the remote bluetooth controller
 */
public class ChangeBTControllerBaudRateRequest extends BaseUsbRequest {


    /**
     * The default length of a baud value.
     */
    private static final int LENGTH_BAUD_RATE_VALUE = 4;

    /**
     * The baud rate value that you want to set to the bt controller.
     */
    private int mBaudRate;


    /**
     * Add a callback to the current request to listen the status of sending and receiving.
     *
     * @param callback Callback for status listening
     */
    public void addChangeBTControllerBaudRateRequestCallback(ChangeBTControllerBaudRateRequestCallback callback) {
        this.mBaseRequestCallback = callback;
    }

    /**
     * Get the callback that is currently used to listen the status.
     *
     * @return Callback for status listening
     */
    public ChangeBTControllerBaudRateRequestCallback getChangeBTControllerBaudRateRequestCallback() {
        return (ChangeBTControllerBaudRateRequestCallback) mBaseRequestCallback;
    }


    /**
     * Use this constructor to crate a request to change the baud rate of bt controller.
     *
     * @param baudRate The baud rate value that you want to set to the bt controller.
     */
    public ChangeBTControllerBaudRateRequest(int baudRate) {
        this.mBaudRate = baudRate;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.READ_LOCAL_VERSION_INFORMATION;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE_FIELD
                + UsbCmdParamLengthDefine.LENGTH_PARAMETER_TOTAL_LEN_FIELD + LENGTH_BAUD_RATE_VALUE;
    }

    @Override
    public void createRequest() {
        super.createRequest();

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID Note: Report ID = 5 in Download Patch in normal mode
        mSendReportID = UsbConfig.REPORT_ID_4;
        byteBuffer.put(mSendReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mSendMessageLength);

        /// Put USB PDU
        // Usb opcode
        byteBuffer.putShort(2, request_opcode);
        // Parameter total length
        byteBuffer.put(4, (byte) LENGTH_BAUD_RATE_VALUE);
        // Specific Baud rate value
        byteBuffer.putInt(5, mBaudRate);
    }

    @Override
    public void parseResponse(byte[] responseData) {
        super.parseResponse(responseData);
        if (response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            if (getChangeBTControllerBaudRateRequestCallback() != null) {
                getChangeBTControllerBaudRateRequestCallback().onModifySuccess();
            }
        } else {
            if (getChangeBTControllerBaudRateRequestCallback() != null) {
                getChangeBTControllerBaudRateRequestCallback().onModifyFailed();
            }
        }
    }

}
