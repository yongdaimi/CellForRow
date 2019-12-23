package com.realsil.sdk.core.usb.connector.att;

/**
 * A callback interface for listening notification and indication message from the server.
 *
 * @author xp.chen
 */
public interface OnServerTransactionChangeCallback {
    /**
     * This method will be triggered when a notification message is returned from the server.
     * @param notificationData Unsolicited PDUs sent to a client by a server that do not
     * invoke a confirmation.
     */
    void onReceiveNotificationMessage(byte[] notificationData);

    /**
     * This method will be triggered when an indication message is received from the server.
     * @param indicationData Unsolicited PDUs sent to a client by a server that invoke a
     * confirmation.
     */
    void onReceiveIndicationMessage(byte[] indicationData);

    /**
     * This method is called if a USB device disconnection is detected.
     */
    void onDeviceDisconnected();

    /**
     * If a write request does not receive the corresponding write response
     */
    void onReceiveWriteResponseTimeout();


}
