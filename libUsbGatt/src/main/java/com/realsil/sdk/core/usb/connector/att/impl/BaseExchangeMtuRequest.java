package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.callback.ExchangeMtuRequestCallback;

/**
 * An abstract class template for creating exchange mtu request
 *
 * @author xp.chen
 */
public abstract class BaseExchangeMtuRequest extends BaseAttributeRequest {


    /**
     * Add a callback interface to listen the status when the client send a {@link ExchangeMtuRequest} to server.
     *
     * @param exchangeMtuRequestCallback A callback is used to listen the status when the client sends a exchange MTU request to server.
     */
    public void addExchangeMtuRequestCallback(ExchangeMtuRequestCallback exchangeMtuRequestCallback) {
        this.mBaseRequestCallback = exchangeMtuRequestCallback;
    }


    /**
     * Get the callback currently used to listen for {@link ExchangeMtuRequestCallback}.
     *
     * @return A Callback currently for listening to {@link ExchangeMtuRequestCallback}.
     */
    public ExchangeMtuRequestCallback getExchangeMtuRequestCallback() {
        return (ExchangeMtuRequestCallback) mBaseRequestCallback;
    }


}
