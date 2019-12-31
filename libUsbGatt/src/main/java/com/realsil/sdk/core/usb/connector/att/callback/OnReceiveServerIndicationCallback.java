package com.realsil.sdk.core.usb.connector.att.callback;

/**
 * A callback interface to listen the indication form the server.
 *
 * @author xp.chen
 */
public interface OnReceiveServerIndicationCallback {

    /**
     * This method will be called when receive a indication message from server.
     * @param indicationData received notification data.
     */
    void onReceiveServerIndication(byte[] indicationData);


}
