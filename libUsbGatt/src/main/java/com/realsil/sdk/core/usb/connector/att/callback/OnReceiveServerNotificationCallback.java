package com.realsil.sdk.core.usb.connector.att.callback;

/**
 * A callback interface to listen the notification form the server.
 *
 * @author xp.chen
 */
public interface OnReceiveServerNotificationCallback {
    /**
     * This method will be called when receive a notification message from server.
     *
     * @param notificationData received notification data.
     */
    void onReceiveServerNotification(byte[] notificationData);

}
