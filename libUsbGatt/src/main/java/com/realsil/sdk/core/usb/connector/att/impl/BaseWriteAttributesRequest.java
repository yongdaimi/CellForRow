package com.realsil.sdk.core.usb.connector.att.impl;

/**
 * An abstract class template for creating Write Attribute Request
 *
 * @author xp.chen
 */
abstract class BaseWriteAttributesRequest extends BaseWriteAttributes {

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
     *
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_SUCCESS
     * @see com.realsil.sdk.core.usb.connector.att.AttributeParseResult#PARSE_FAILED
     */
    public abstract int parseResponse(byte[] response);

}
