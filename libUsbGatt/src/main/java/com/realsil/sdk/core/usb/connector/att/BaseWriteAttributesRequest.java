package com.realsil.sdk.core.usb.connector.att;

/**
 * An abstract class template for creating Write Attribute Request
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
     */
    public abstract void parseResponse(byte[] response);

}
