package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.BaseRequest;
import com.realsil.sdk.core.usb.connector.att.AttPduErrorCodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An abstract class template for creating ATT PDU Request.
 *
 * @author xp.chen
 */
public abstract class BaseAttributeRequest extends BaseRequest {

    /**
     * The request opcode sent by client. It will be included in the request pdu to be sent, defined in {@link AttPduOpcodeDefine}.
     *
     * @see AttPduOpcodeDefine#READ_REQUEST
     * @see AttPduOpcodeDefine#WRITE_REQUEST
     */
    byte request_opcode;

    /**
     * The response opcode return by server. If server processing fails, it will be equal to {@link AttPduOpcodeDefine#ERROR_RESPONSE}.
     */
    byte response_opcode;

    /**
     * The request opcode that generated this error response.
     */
    byte error_request_opcode;

    /**
     * The attribute handle that generated this error response
     */
    short error_att_handle;

    /**
     * The reason why the request has generated an error response, it is defined in class {@link AttPduErrorCodeDefine}.
     *
     * @see AttPduErrorCodeDefine
     */
    byte error_code;

    /**
     * Define a default parse result, default value is {@link AttributeParseResult#PARSE_FAILED}
     */
    int mParseResult = AttributeParseResult.PARSE_FAILED;

    /**
     * Returns the parsing result of the response pdu returned by the server
     *
     * @return parsing result. parse success: it will be {@link AttributeParseResult#PARSE_SUCCESS};
     * parse failed: it will be {@link AttributeParseResult#PARSE_FAILED}
     */
    public final int getParseResult() {
        return mParseResult;
    }

    /**
     * Call this method to get the request opcode sent by client.
     *
     * @return request code.
     * @see AttPduOpcodeDefine
     */
    public byte getRequestOpcode() {
        return request_opcode;
    }

    /**
     * Use this method to create a Write Attributes Request.
     */
    @Override
    public void createRequest() {
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mSendMessageLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = selectComfortableReportID(mSendDataLength);
    }

    /**
     * Parse the response returned by the server in this method.
     *
     * @param response response data from server.
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_SUCCESS
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_FAILED
     */
    @Override
    public void parseResponse(byte[] response) {
        response_opcode = response[2];
        mReceiveMessageLength = response[1] & 0x0FF;
    }

}
