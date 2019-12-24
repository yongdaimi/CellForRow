package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;

/**
 * An abstract class template for creating Write Attribute Request
 *
 * @author xp.chen
 */
abstract class BaseWriteAttributeRequest extends BaseAttributeRequest {

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
        return (WriteAttributeRequestCallback)mBaseRequestCallback;
    }


}
