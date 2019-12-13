package com.realsil.sdk.core.usb;



/**
 * This abstract class is used to implement {@link UsbGatt} callbacks.
 * @author bingshanguxue
 */
public abstract class UsbGattCallback {
    /**
     * Callback indicating when GATT client has connected/disconnected to/from a remote
     * GATT server.
     *
     * @param gatt GATT client
     * @param status Status of the connect or disconnect operation. {@link
     * UsbGatt#GATT_SUCCESS} if the operation succeeds.
     * @param newState Returns the new connection state. Can be one of {@link
     * UsbGatt#STATE_DISCONNECTED} or {@link UsbGatt#STATE_CONNECTED}
     */
    public void onConnectionStateChange(UsbGatt gatt, int status,
                                        int newState) {
    }


    /**
     * Callback invoked when the list of remote services, characteristics and descriptors
     * for the remote device have been updated, ie new services have been discovered.
     *
     * @param gatt GATT client invoked {@link UsbGatt#discoverServices}
     * @param status {@link UsbGatt#GATT_SUCCESS} if the remote device has been explored
     * successfully.
     */
    public void onServicesDiscovered(UsbGatt gatt, int status) {
    }

    /**
     * Callback reporting the result of a characteristic read operation.
     *
     * @param gatt GATT client invoked {@link UsbGatt#readCharacteristic}
     * @param characteristic Characteristic that was read from the associated remote device.
     * @param status {@link UsbGatt#GATT_SUCCESS} if the read operation was completed
     * successfully.
     */
    public void onCharacteristicRead(UsbGatt gatt, UsbGattCharacteristic characteristic,
                                     int status) {
    }

    /**
     * Callback indicating the result of a characteristic write operation.
     *
     * <p>If this callback is invoked while a reliable write transaction is
     * in progress, the value of the characteristic represents the value
     * reported by the remote device. An application should compare this
     * value to the desired value to be written. If the values don't match,
     * the application must abort the reliable write transaction.
     *
     * @param gatt GATT client invoked {@link UsbGatt#writeCharacteristic}
     * @param characteristic Characteristic that was written to the associated remote device.
     * @param status The result of the write operation {@link UsbGatt#GATT_SUCCESS} if the
     * operation succeeds.
     */
    public void onCharacteristicWrite(UsbGatt gatt,
                                      UsbGattCharacteristic characteristic, int status) {
    }

    /**
     * Callback triggered as a result of a remote characteristic notification.
     *
     * @param gatt GATT client the characteristic is associated with
     * @param characteristic Characteristic that has been updated as a result of a remote
     * notification event.
     */
    public void onCharacteristicChanged(UsbGatt gatt,
                                        UsbGattCharacteristic characteristic) {
    }

    /**
     * Callback indicating the MTU for a given device connection has changed.
     *
     * This callback is triggered in response to the
     * {@link UsbGatt#requestMtu} function, or in response to a connection
     * event.
     *
     * @param gatt GATT client invoked {@link UsbGatt#requestMtu}
     * @param mtu The new MTU size
     * @param status {@link UsbGatt#GATT_SUCCESS} if the MTU has been changed successfully
     */
    public void onMtuChanged(UsbGatt gatt, int mtu, int status) {
    }
}
