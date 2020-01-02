package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

public abstract class ExchangeMtuRequestCallback extends BaseRequestCallback {


    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.cmd.impl.ExchangeMtuRequest is sent successfully
     * and received server's mtu size.
     * @param serverMtuSize attribute server receive MTU size.
     */
    public void onReceiveServerRxMtu(int serverMtuSize){};

    public void onReceiveFailed(){};

}
