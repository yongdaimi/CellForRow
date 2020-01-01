package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

public abstract class QueryBTConnectStateRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when received bt connect state.
     *
     * @param connectState true: has connected, false, has disconnected.
     */
    public void onReceiveConnectState(boolean connectState) {}

}
