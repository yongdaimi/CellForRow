package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttributeOpcode;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.callback.BaseRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An abstract class template for creating a pdu with a response
 *
 * @author xp.chen
 */
public abstract class BaseAttributeRequest extends BaseAttributeProtocol {

    /**
     * The request opcode sent by client. It will be included in the request pdu to be sent, defined in {@link AttributeOpcode}.
     *
     * @see AttributeOpcode#READ_REQUEST
     * @see AttributeOpcode#WRITE_REQUEST
     */
    byte request_opcode;

    /**
     * The response opcode return by server. If server processing fails, it will be equal to {@link AttributeOpcode#ERROR_RESPONSE}.
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
     * The reason why the request has generated an error response, it is defined in class {@link com.realsil.sdk.core.usb.connector.att.AttributeErrorCode}.
     *
     * @see com.realsil.sdk.core.usb.connector.att.AttributeErrorCode
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
     * A callback is used to listen the data sending status when the client sends request data to the server.
     */
    BaseRequestCallback mBaseRequestCallback;

    /**
     * Get the callback currently used to listen for {@link BaseRequestCallback}.
     * @return A Callback currently for listening to {@link BaseRequestCallback}.
     */
    public BaseRequestCallback getRequestCallback() {
        return mBaseRequestCallback;
    }

    /**
     * Call this method to set internal {@link BaseAttributeRequest#request_opcode} member variables.
     */
    public abstract void setRequestOpcode();

    /**
     * Call this method to get the request opcode sent by client.
     *
     * @return request code.
     * @see AttributeOpcode
     */
    public byte getRequestOpcode() {
        return request_opcode;
    }

    /**
     * Use this method to create a Write Attributes Request.
     */
    public abstract void createRequest();

    /**
     * Parse the response returned by the server in this method.
     *
     * @param response response data from server.
     * @return Parse result, May be {@link com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_SUCCESS}
     * or {@link com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_FAILED}.
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_SUCCESS
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_FAILED
     */
    public void parseResponse(byte[] response) {
        response_opcode = response[0];
        if (response_opcode == AttributeOpcode.ERROR_RESPONSE) {
            ByteBuffer buffer = ByteBuffer.wrap(response);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            error_request_opcode = buffer.get(1);
            error_att_handle = buffer.get(2);
            error_code = buffer.get(4);
        }
    }

}
