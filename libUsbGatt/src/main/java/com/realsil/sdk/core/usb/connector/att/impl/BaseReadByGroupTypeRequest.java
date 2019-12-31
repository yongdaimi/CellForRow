package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.callback.ReadByGroupTypeRequestCallback;

/**
 * An abstract class template for creating Read by group type request
 *
 * @author xp.chen
 */
public abstract class BaseReadByGroupTypeRequest extends BaseAttributeRequest {

    /**
     * Add a callback interface to listen the status when the client send a {@link ReadByGroupTypeRequest} to server.
     *
     * @param readByGroupTypeRequestCallback readByGroupTypeRequestCallback instance
     */
    public void addReadByGroupTypeRequestCallback(ReadByGroupTypeRequestCallback readByGroupTypeRequestCallback) {
        this.mBaseRequestCallback = readByGroupTypeRequestCallback;
    }

    /**
     * Get the callback currently used to listen fro {@link ReadByGroupTypeRequest}
     *
     * @return A Callback currently for listening to {@link ReadByGroupTypeRequest}.
     */
    public ReadByGroupTypeRequestCallback getReadByGroupTypeRequestCallback() {
        return (ReadByGroupTypeRequestCallback) mBaseRequestCallback;
    }

}
