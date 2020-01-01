package com.realsil.sdk.core.usb.connector.att.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * A callback method is used to listen the status that Client writes attribute to server.
 *
 * @author xp.chen
 */
public abstract class WriteAttributeRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest}is sent successfully
     * and also received a write response from the server
     */
    public void onWriteSuccess() {}

}
