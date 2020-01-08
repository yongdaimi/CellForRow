package com.realsil.sdk.core.usb.connector.callback;

import android.app.Notification;
import android.hardware.usb.UsbDevice;

/**
 * A callback is used to listen the status of the connected USB device, such as the device is authorized, the device is
 * connected, the device is disconnected, etc.
 *
 * @author xp.chen
 */
public abstract class OnUsbDeviceStatusChangeCallback {

    /**
     * This method will be called when the usb device has authorized.
     *
     * @param usbDevice       Currently authorized devices
     * @param authorizeResult true, User agrees to authorize the device; false: User refuses to authorize the device
     */
    public void authorizeCurrentDevice(UsbDevice usbDevice, boolean authorizeResult) {}


    /**
     * This method will be called when the usb device's connection status has changed.
     *
     * @param connectionStatus true: If usb device has connected, false: If usb device has disconnected.
     */
    public void onDeviceConnectionStatusHasChanged(boolean connectionStatus) {}

    /**
     * This method will be called when receive a notification of an attribute's value.
     */
    public void onReceiveHandleValueNotification(short att_handle, byte[] att_value) {}

    /**
     * his method will be called when the usb device's running status has changed, tt will contain a current status code and related detail info.
     *
     * @param errorCode  Identification code when status changes
     * @param detailInfo Detail info when status changes
     */
    public void onDeviceStatusChange(int errorCode, String detailInfo) {}


}
