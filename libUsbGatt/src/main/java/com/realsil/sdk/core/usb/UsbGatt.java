package com.realsil.sdk.core.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Public API for the USB GATT Profile.
 *
 * <p>This class provides USB GATT functionality to enable communication
 * with USB Smart or Smart Ready devices.
 *
 * <p>To connect to a remote peripheral device, create a {@link UsbGattCallback}
 * and call {@link UsbGattImpl#connectGatt} to get a instance of this class.
 * GATT capable devices can be discovered using the Bluetooth device discovery or BLE
 * scan process.
 * @author bingshanguxue
 */
public class UsbGatt {
    private static final String TAG = "UsbGatt";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /** The profile is in disconnected state */
    public static final int STATE_DISCONNECTED = 0;
    /** The profile is in connecting state */
    public static final int STATE_CONNECTING = 1;
    /** The profile is in connected state */
    public static final int STATE_CONNECTED = 2;
    /** The profile is in disconnecting state */
    public static final int STATE_DISCONNECTING = 3;

    private static final int CONN_STATE_IDLE = 0;
    private static final int CONN_STATE_CONNECTING = 1;
    private static final int CONN_STATE_CONNECTED = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_CLOSED = 4;

    /** A GATT operation completed successfully */
    public static final int GATT_SUCCESS = 0;

    /** GATT read operation is not permitted */
    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    /** GATT write operation is not permitted */
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    /** Insufficient authentication for a given operation */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    /** The given request is not supported */
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    /** Insufficient encryption for a given operation */
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    /** A read or write operation was requested with an invalid offset */
    public static final int GATT_INVALID_OFFSET = 0x7;

    /** A write operation exceeds the maximum length of the attribute */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    /** A remote device connection is congested. */
    public static final int GATT_CONNECTION_CONGESTED = 0x8f;

    /** A GATT operation failed, errors other than the above */
    public static final int GATT_FAILURE = 0x101;

    private Context mContext = null;
    private UsbDevice mDevice;
    private List<UsbGattCharacteristic> mCharacteristics;
    
    

    private final Object mStateLock = new Object();
    private int mConnState = CONN_STATE_IDLE;

    public UsbGatt(UsbDevice mDevice) {
        this.mDevice = mDevice;
        mConnState = CONN_STATE_IDLE;
    }


    /**
     * Close this USB GATT client.
     * <p>
     * Application should call this method as early as possible after it is done with
     * this GATT client.
     */
    public void close() {
        if (DBG) {
            Log.d(TAG, "close()");
        }

//        unregisterApp();
        mConnState = CONN_STATE_CLOSED;
//        mAuthRetryState = AUTH_RETRY_STATE_IDLE;
    }

    /**
     * Returns a list of GATT services offered by the remote device.
     *
     * <p>This function requires that service discovery has been completed
     * for the given device.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return List of services on the remote device. Returns an empty list if service discovery has
     * not yet been performed.
     */
    public List<UsbGattCharacteristic> getCharacteristics() {
        List<UsbGattCharacteristic> result =
                new ArrayList<>();

        for (UsbGattCharacteristic service : mCharacteristics) {
//            if (service.getDevice().equals(mDevice)) {
                result.add(service);
//            }
        }

        return result;
    }

    /**
     * Returns a {@link UsbGattCharacteristic}, if the requested UUID is
     * supported by the remote device.
     *
     * <p>This function requires that service discovery has been completed
     * for the given device.
     *
     * <p>If multiple instances of the same service (as identified by UUID)
     * exist, the first instance of the service is returned.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested service is not offered by
     * the remote device.
     */
    public UsbGattCharacteristic getCharacteristic(UUID uuid) {
        for (UsbGattCharacteristic service : mCharacteristics) {
//            if (service.getDevice().equals(mDevice) && service.getUuid().equals(uuid)) {
            if (service.getUuid().equals(uuid)) {
                return service;
            }
        }

        return null;
    }

    /**
     * Reads the requested characteristic from the associated remote device.
     *
     * <p>This is an asynchronous operation. The result of the read operation
     * is reported by the {@link UsbGattCallback#onCharacteristicRead}
     * callback.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to read from the remote device
     * @return true, if the read operation was initiated successfully
     */
    public boolean readCharacteristic(UsbGattCharacteristic characteristic) {
        if (VDBG) {
            Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        }




        // TODO: 2019-12-04
//        if (mService == null || mClientIf == 0) return false;
//
//        BluetoothGattService service = characteristic.getService();
//        if (service == null) return false;
//
//        BluetoothDevice device = service.getDevice();
//        if (device == null) return false;
//
//        synchronized (mDeviceBusy) {
//            if (mDeviceBusy) return false;
//            mDeviceBusy = true;
//        }
//
//        try {
//            mService.readCharacteristic(mClientIf, device.getAddress(),
//                    characteristic.getInstanceId(), AUTHENTICATION_NONE);
//        } catch (RemoteException e) {
//            Log.e(TAG, "", e);
//            mDeviceBusy = false;
//            return false;
//        }

        return true;
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * <p>Once the write operation has been completed, the
     * {@link UsbGattCallback#onCharacteristicWrite} callback is invoked,
     * reporting the result of the operation.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param characteristic Characteristic to write on the remote device
     * @return true, if the write operation was initiated successfully
     */
    public boolean writeCharacteristic(UsbGattCharacteristic characteristic) {
        if (characteristic.getValue() == null) {
            return false;
        }

        if (VDBG) {
            Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        }

        // Add(xp.chen)
        int writeType = characteristic.getWriteType();
        switch (writeType) {
            case UsbGattCharacteristic.WRITE_TYPE_DEFAULT:

                break;
            case UsbGattCharacteristic.WRITE_TYPE_NO_RESPONSE:

                break;
            case UsbGattCharacteristic.WRITE_TYPE_SIGNED:

                break;
            default:
                break;
        }


        // TODO: 2019-12-04
//        transparentTransport(...)

        return true;
    }

    /**
     * Initiate a connection to a Usb GATT capable device.
     *
     * <p>The connection may not be established right away, but will be
     * completed when the remote device is available. A
     * {@link UsbGattCallback#onConnectionStateChange} callback will be
     * invoked when the connection state changes as a result of this function.
     *
     * <p>The autoConnect parameter determines whether to actively connect to
     * the remote device, or rather passively scan and finalize the connection
     * when the remote device is in range/available. Generally, the first ever
     * connection to a device should be direct (autoConnect set to false) and
     * subsequent connections to known devices should be invoked with the
     * autoConnect parameter set to true.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device      Remote device to connect to
     * @param autoConnect Whether to directly connect to the remote device (false) or to
     *                    automatically connect as soon as the remote device becomes available (true).
     * @return true, if the connection attempt was initiated successfully
     */
    boolean connect(UsbGattCallback callback) {
        if (DBG) {
            Log.d(TAG,
                    "connect() - device: " + mDevice.getDeviceName());
        }
        synchronized (mStateLock) {
            if (mConnState != CONN_STATE_IDLE) {
                throw new IllegalStateException("Not idle");
            }
            mConnState = CONN_STATE_CONNECTING;
        }

        // TODO: 2019-12-04
        return true;
    }


    /**
     * Connect back to remote device.
     *
     * <p>This method is used to re-connect to a remote device after the
     * connection has been dropped. If the device is not in range, the
     * re-connection will be triggered once the device is back in range.
     *
     * @return true, if the connection attempt was initiated successfully
     */
    public boolean connect() {
        // TODO: 2019-12-04
        return true;
    }

    /**
     * Disconnects an established connection, or cancels a connection attempt
     * currently in progress.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     */
    public void disconnect() {
        if (DBG) {
            Log.d(TAG, "cancelOpen() - device: " + mDevice.getDeviceName());
        }
        // TODO: 2019-12-04
    }


    /**
     * Return the remote Usb device this GATT client targets to
     *
     * @return remote Usb device
     */
    public UsbDevice getDevice() {
        return mDevice;
    }

    /**
     * Discovers services offered by a remote device as well as their
     * characteristics and descriptors.
     *
     * <p>This is an asynchronous operation. Once service discovery is completed,
     * the {@link UsbGattCallback#onServicesDiscovered} callback is
     * triggered. If the discovery was successful, the remote services can be
     * retrieved using the {@link #getServices} function.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the remote service discovery has been started
     */
    public boolean discoverServices() {
        if (DBG) {
            Log.d(TAG, "discoverServices() - device: " + mDevice.getDeviceName());
        }

        mCharacteristics.clear();

        // TODO: 2019-12-04
        return true;
    }


    /**
     * Request an MTU size used for a given connection.
     *
     * <p>When performing a write request operation (write without response),
     * the data sent is truncated to the MTU size. This function may be used
     * to request a larger MTU size to be able to send more data at once.
     *
     * <p>A {@link UsbGattCallback#onMtuChanged} callback will indicate
     * whether this operation was successful.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the new MTU value has been requested successfully
     */
    public boolean requestMtu(int mtu) {
        if (DBG) {
            Log.d(TAG, "configureMTU() - device: " + mDevice.getDeviceName()
                    + " mtu: " + mtu);
        }

        // TODO: 2019-12-10

        return true;
    }

    /**
     * @param reportId
     * @param length
     * @param opcode
     * @param parameters
     * @return
     */
    public boolean transparentTransport(byte reportId, int length, byte opcode, byte[] parameters) {
        // TODO: 2019-12-10
        return true;
    }

}
