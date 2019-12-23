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
    public int parseResponse(byte[] response) {
        byte att_opcode = response[0];
        if (att_opcode == AttributeOpcode.READ_RESPONSE) {
            if (mOnClientTransactionChangeCallback != null) {
                mOnClientTransactionChangeCallback.onReceiveSuccess();
            }
            return AttributeParseResult.PARSE_SUCCESS;
        } else if (att_opcode == AttributeOpcode.ERROR_RESPONSE) {
            ByteBuffer buffer = ByteBuffer.wrap(response);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            byte request_opcode_in_error = buffer.get(1);
            short att_handle_in_error = buffer.get(2);
            byte error_code = buffer.get(4);
            if (mOnClientTransactionChangeCallback != null)
                mOnClientTransactionChangeCallback.onReceiveFailed(att_opcode, request_opcode_in_error, att_handle_in_error, error_code);
        }
        return AttributeParseResult.PARSE_FAILED;
    }

}
