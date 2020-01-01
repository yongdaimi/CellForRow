package com.realsil.sdk.core.usb.connector.att.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * A callback method is used to listen the status that client send {@link com.realsil.sdk.core.usb.connector.att.impl.ExchangeMtuRequest}
 * to server.
 * @author xp.chen
 */
public abstract class ExchangeMtuRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.ExchangeMtuRequest} is sent successfully
     * and received server's mtu size.
     * @param serverMtuSize attribute server receive MTU size.
     */
    public void onReceiveServerRxMtu(int serverMtuSize) {}

}
