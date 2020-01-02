package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

public abstract class QueryBTConnectStateRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when received bt connect state.
     *
     * @param statusCode Status code of the current command, 0 means success, other means failure.
     * @param connectState true: has connected, false, has disconnected.
     */
    public void onReceiveConnectState(int statusCode, boolean connectState) {}

}
