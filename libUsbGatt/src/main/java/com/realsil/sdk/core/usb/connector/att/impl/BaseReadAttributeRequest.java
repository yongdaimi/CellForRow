package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.callback.ReadAttributeRequestCallback;

/**
 * An abstract class template for creating Read Attribute Request.
 *
 * @author xp.chen
 */
abstract class BaseReadAttributeRequest extends BaseAttributeRequest {


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


}
