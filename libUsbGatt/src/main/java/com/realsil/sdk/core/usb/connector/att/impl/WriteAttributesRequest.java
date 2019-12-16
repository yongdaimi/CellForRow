package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttributeOpcode;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Write Request is used to request the server to write the value of an
 * attribute and acknowledge that this has been achieved in a Write Response.
 *
 * @author xp.chen
 */
public class WriteAttributesRequest extends BaseWriteAttributesRequest {


    /**
     * Use this constructor to create a Write Attributes Request.
     *
     * @param attHandle The handler of the attribute to be written.
     * @param attValue  The value to be written to the attribute.
     */
    public WriteAttributesRequest(short attHandle, byte[] attValue) {
        this.mAttHandle = attHandle;
        this.mAttValue = attValue;
    }

    @Override
    public void createRequest() {
        this.mAttPduLength = LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE + mAttValue.length;
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
        byteBuffer.put(2, AttributeOpcode.WRITE_REQUEST);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
        // Att value
        System.arraycopy(mAttValue, 0, mSendData, 5, mAttValue.length);
    }

    @Override
    public int parseResponse(byte[] response) {
        if (response != null && response.length == 1) {
            byte att_opcode = response[0];
            if (att_opcode == AttributeOpcode.WRITE_RESPONSE) {
                if (mAttributeCommCallback != null) {
                    mAttributeCommCallback.onReceiveSuccess();
                }
                return AttributeParseResult.PARSE_SUCCESS;
            } else {
                ByteBuffer buffer = ByteBuffer.wrap(response);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                byte request_opcode_in_error = buffer.get(1);
                short att_handle_in_error = buffer.get(2);
                byte error_code = buffer.get(4);
                if (mAttributeCommCallback != null)
                    mAttributeCommCallback.onReceiveFailed(att_opcode, request_opcode_in_error, att_handle_in_error, error_code);
            }
        }
        return AttributeParseResult.PARSE_FAILED;
    }

}
