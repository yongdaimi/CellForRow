package com.realsil.sdk.core.usb;

import android.hardware.usb.UsbDevice;

/**
 * @author bingshanguxue
 */
public class UsbGattImpl {
    /**
     * Connect to GATT Server hosted by this device. Caller acts as GATT client.
     * The callback is used to deliver results to Caller, such as connection status as well
     * as any further GATT client operations.
     * The method returns a BluetoothGatt instance. You can use BluetoothGatt to conduct
     * GATT client operations.
     *
     * @param callback GATT callback handler that will receive asynchronous callbacks.
     * @return A UsbGatt instance. You can use UsbGatt to conduct GATT client
     * operations.
     * @hide
     */
    public static UsbGatt connectGatt(UsbDevice device, UsbGattCallback callback) {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }

        UsbGatt gatt = new UsbGatt(device);
        gatt.connect(callback);
        return gatt;
    }
}
