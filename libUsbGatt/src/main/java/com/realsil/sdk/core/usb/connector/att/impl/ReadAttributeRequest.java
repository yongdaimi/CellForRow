package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttributeOpcode;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Read Request is used to request the server to read the value of an
 * attribute and return its value in a Read Response
 *
 * @author xp.chen
 */
public class ReadAttributeRequest extends BaseReadAttributeRequest {


    /**
     * Use this constructor to create a Read Attributes Request.
     *
     * @param attHandle The handler of the attribute to be read.
     */
    public ReadAttributeRequest(short attHandle, byte[] attValue) {
        this.mAttHandle = attHandle;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttributeOpcode.READ_REQUEST;
    }

    @Override
    public void createRequest() {
        this.mAttPduLength = LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE;
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mAttPduLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = selectComfortableReportID(mSendDataLength);

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mAttPduLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, AttributeOpcode.READ_REQUEST);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttributeOpcode.READ_RESPONSE) {
            byte[] att_value;
            if (response.length > 1) {
                att_value = new byte[response.length - 1];
                System.arraycopy(response, 1, att_value, 0, att_value.length);
            } else {
                att_value = new byte[0];
            }

            if (getReadAttributeRequestCallback() != null) {
                getReadAttributeRequestCallback().onReadSuccess(att_value);
            }
            mParseResult = AttributeParseResult.PARSE_SUCCESS;
        } else {
            if (getReadAttributeRequestCallback() != null)
                getReadAttributeRequestCallback().onReceiveFailed(response_opcode, error_request_opcode, error_att_handle, error_code);
        }
    }

}
