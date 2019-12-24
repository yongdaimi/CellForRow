package com.realsil.sdk.core.usb.connector.att.callback;

/**
 * A callback method is used to listen the status that client sends a read request to the server.
 *
 * @author xp.chen
 */
public abstract class ReadAttributeRequestCallback extends BaseRequestCallback{

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest}is sent successfully
     * and received a read response from the server.
     *
     * @param attributeValue The value of the attribute with the handle given.
     */
    public void onReadSuccess(byte[] attributeValue) {}

}
