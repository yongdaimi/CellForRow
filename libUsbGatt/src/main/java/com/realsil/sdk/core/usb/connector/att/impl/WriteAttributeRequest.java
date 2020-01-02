package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttPduParamLengthDefine;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Write Request is used to request the server to write the value of an
 * attribute and acknowledge that this has been achieved in a Write Response.
 *
 * @author xp.chen
 */
public class WriteAttributeRequest extends BaseAttributeRequest {

    /**
     * The handler of the attribute to be written or read.
     */
    private short mAttHandle;

    /**
     * The value of the attribute to be written.
     */
    private byte[] mAttValue;

    /**
     * Use this constructor to create a Write Attributes Request.
     *
     * @param attHandle The handler of the attribute to be written.
     * @param attValue  The value to be written to the attribute.
     */
    public WriteAttributeRequest(short attHandle, byte[] attValue) {
        this.mAttHandle = attHandle;
        this.mAttValue = attValue;
    }

    /**
     * Add a callback interface to listen the status of data sent by the client to the server
     *
     * @param writeAttributeRequestCallback A callback is used to listen the data sending status when the client sends data to the server.
     */
    public void addWriteAttributeRequestCallback(WriteAttributeRequestCallback writeAttributeRequestCallback) {
        this.mBaseRequestCallback = writeAttributeRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link WriteAttributeRequest}.
     *
     * @return A Callback currently for listening to {@link WriteAttributeRequest}.
     */
    public WriteAttributeRequestCallback getWriteAttributeRequestCallback() {
        return (WriteAttributeRequestCallback) mBaseRequestCallback;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttPduOpcodeDefine.WRITE_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = AttPduParamLengthDefine.LENGTH_ATT_OPCODE + AttPduParamLengthDefine.LENGTH_ATT_HANDLE + mAttValue.length;
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

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, request_opcode);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
        // Att value
        System.arraycopy(mAttValue, 0, mSendData, 5, mAttValue.length);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttPduOpcodeDefine.WRITE_RESPONSE) {
            if (getWriteAttributeRequestCallback() != null) {
                getWriteAttributeRequestCallback().onWriteSuccess();
            }
            mParseResult = AttributeParseResult.PARSE_SUCCESS;
        } else {
            if (getWriteAttributeRequestCallback() != null) {
                getWriteAttributeRequestCallback().onReceiveFailed(response_opcode, error_request_opcode, error_att_handle, error_code);
            }
        }
    }

}
