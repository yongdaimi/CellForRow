package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttPduParamLengthDefine;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.callback.ReadAttributeRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Read Request is used to request the server to read the value of an
 * attribute and return its value in a Read Response
 *
 * @author xp.chen
 */
public class ReadAttributeRequest extends BaseAttributeRequest {


    /**
     * The handler of the attribute to be written or read.
     */
    private short mAttHandle;

    /**
     * Use this constructor to create a Read Attributes Request.
     *
     * @param attHandle The handler of the attribute to be read.
     */
    public ReadAttributeRequest(int attHandle) {
        this.mAttHandle = (short) attHandle;
    }

    /**
     * Add a callback interface to listen the status when the client sends a {@link ReadAttributeRequest} to the server.
     *
     * @param readAttributeRequestCallback A callback is used to listen the data sending status when the client sends a read attribute request to the server.
     */
    public void addReadAttributeRequestCallback(ReadAttributeRequestCallback readAttributeRequestCallback) {
        this.mBaseRequestCallback = readAttributeRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link ReadAttributeRequest}.
     *
     * @return A Callback currently for listening to {@link ReadAttributeRequest}.
     */
    public ReadAttributeRequestCallback getReadAttributeRequestCallback() {
        return (ReadAttributeRequestCallback) mBaseRequestCallback;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttPduOpcodeDefine.READ_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mMessageLength = AttPduParamLengthDefine.LENGTH_ATT_OPCODE + AttPduParamLengthDefine.LENGTH_ATT_HANDLE;
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
        byteBuffer.put(1, (byte) mMessageLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, request_opcode);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttPduOpcodeDefine.READ_RESPONSE) {
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
            if (getReadAttributeRequestCallback() != null) {
                getReadAttributeRequestCallback().onReceiveFailed(response_opcode, error_request_opcode, error_att_handle, error_code);
            }
        }
    }

}
