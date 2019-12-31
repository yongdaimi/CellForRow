package com.realsil.sdk.core.usb.connector.callback;

/**
 * A callback is used to listen the status of the connected USB device, such as the device is authorized, the device is
 * connected, the device is disconnected, etc.
 *
 * @author xp.chen
 */
public interface OnUsbDeviceStatusChangeCallback {

    /**
     * This method will be called when the usb device has authorized.
     * @param authorizeResult true, User agrees to authorize the device; false: User refuses to authorize the device
     */
    void authorizeCurrentDevice(boolean authorizeResult);

    /**
     * This method will be called when the usb device has connected.
     */
    void onDeviceHasConnected();

    /**
     * This method will be called when the usb device has disconnected.
     */
    void onDeviceHasDisconnected();

    /**
     * his method will be called when the usb device's running status has changed, tt will contain a current status code and related detail info.
     * @param errorCode Identification code when status changes
     * @param detailInfo Detail info when status changes
     */
    void onDeviceStatusChange(int errorCode, String detailInfo);

}
