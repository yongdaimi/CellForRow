package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.callback.ReadByTypeRequestCallback;

/**
 * An abstract class template for creating Read by type request
 *
 * @author xp.chen
 */
public abstract class BaseReadByTypeRequest extends BaseAttributeRequest {

    /**
     * Add a callback interface to listen the status when the client send a {@link ReadByTypeRequest} to server.
     *
     * @param readByTypeRequestCallback callback instance
     */
    public void addReadByTypeRequestCallback(ReadByTypeRequestCallback readByTypeRequestCallback) {
        this.mBaseRequestCallback = readByTypeRequestCallback;
    }

    /**
     * Get the callback currently used to listen fro {@link ReadByTypeRequest}
     *
     * @return A Callback currently for listening to {@link ReadByTypeRequest}.
     */
    public ReadByTypeRequestCallback getReadByTypeRequestCallback() {
        return (ReadByTypeRequestCallback) mBaseRequestCallback;
    }

}
