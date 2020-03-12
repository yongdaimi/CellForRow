package com.realsil.sdk.core.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.UsbError;
import com.realsil.sdk.core.usb.connector.att.callback.ReadAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeCommandCallback;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ExchangeMtuRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadDongleConfigRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.ExchangeMtuRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.QueryBTConnectStateRequest;
import com.realsil.sdk.core.usb.connector.cmd.impl.ReadDongleConfigRequest;

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
 *
 * @author bingshanguxue
 */
public final class UsbGatt {
    private static final String TAG = "UsbGatt";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /**
     * The profile is in disconnected state
     */
    public static final int STATE_DISCONNECTED  = 0;
    /**
     * The profile is in connecting state
     */
    public static final int STATE_CONNECTING    = 1;
    /**
     * The profile is in connected state
     */
    public static final int STATE_CONNECTED     = 2;
    /**
     * The profile is in disconnecting state
     */
    public static final int STATE_DISCONNECTING = 3;

    private static final int CONN_STATE_IDLE          = 0;
    private static final int CONN_STATE_CONNECTING    = 1;
    private static final int CONN_STATE_CONNECTED     = 2;
    private static final int CONN_STATE_DISCONNECTING = 3;
    private static final int CONN_STATE_CLOSED        = 4;

    /**
     * A GATT operation completed successfully
     */
    public static final int GATT_SUCCESS = 0;

    /**
     * GATT read operation is not permitted
     */
    public static final int GATT_READ_NOT_PERMITTED = 0x2;

    /**
     * GATT write operation is not permitted
     */
    public static final int GATT_WRITE_NOT_PERMITTED = 0x3;

    /**
     * Insufficient authentication for a given operation
     */
    public static final int GATT_INSUFFICIENT_AUTHENTICATION = 0x5;

    /**
     * The given request is not supported
     */
    public static final int GATT_REQUEST_NOT_SUPPORTED = 0x6;

    /**
     * Insufficient encryption for a given operation
     */
    public static final int GATT_INSUFFICIENT_ENCRYPTION = 0xf;

    /**
     * A read or write operation was requested with an invalid offset
     */
    public static final int GATT_INVALID_OFFSET = 0x7;

    /**
     * A write operation exceeds the maximum length of the attribute
     */
    public static final int GATT_INVALID_ATTRIBUTE_LENGTH = 0xd;

    /**
     * A remote device connection is congested.
     */
    public static final int GATT_CONNECTION_CONGESTED = 0x8f;

    /**
     * A GATT operation failed, errors other than the above
     */
    public static final int GATT_FAILURE = 0x101;

    private Context mContext = null;

    private UsbDevice mDevice;

    private List<UsbGattCharacteristic> mCharacteristics;


    private final Object mStateLock = new Object();

    private int mConnState = CONN_STATE_IDLE;


    /**
     * Define a member variable to store the UsbGattCallback passed by the {@link UsbGatt#connect(UsbGattCallback)}
     */
    private UsbGattCallback mUsbGattCallback;

    public UsbGatt(UsbDevice mDevice) {
        this.mDevice = mDevice;
        mConnState = CONN_STATE_IDLE;
        mCharacteristics = new ArrayList<UsbGattCharacteristic>();
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
        if (mCharacteristics == null || mCharacteristics.size() <= 0) {
            return null;
        }
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
        if (characteristic == null) {
            return false;
        }
        if (VDBG) {
            Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        }
        // Add(read detail info of attribute by characteristic)
        readAttributeRequest(characteristic);
        return true;
    }


    /**
     * Call this method to read an attribute from the server.
     *
     * @param characteristic characteristic to be read from the server.
     */
    private void readAttributeRequest(UsbGattCharacteristic characteristic) {
        short att_handle = (short) characteristic.getInstanceId();
        final UsbGattCharacteristic read_characteristic = characteristic;

        ReadAttributeRequest readRequest = new ReadAttributeRequest(att_handle);
        readRequest.addReadAttributeRequestCallback(new ReadAttributeRequestCallback() {
            @Override
            public void onReadSuccess(byte[] attributeValue) {
                super.onReadSuccess(attributeValue);
                if (mUsbGattCallback != null) {
                    read_characteristic.setValue(attributeValue);
                    mUsbGattCallback.onCharacteristicRead(UsbGatt.this, read_characteristic, UsbGatt.GATT_SUCCESS);
                }
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicRead(UsbGatt.this, read_characteristic, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onReceiveFailed(att_opcode, request_code, att_handler, error_code);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicRead(UsbGatt.this, read_characteristic, getGattErrorCode(error_code));
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicRead(UsbGatt.this, read_characteristic, UsbGatt.GATT_FAILURE);
                }
            }
        });
        LocalUsbConnector.getInstance().sendRequest(readRequest);
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
        if (characteristic == null || characteristic.getValue() == null) {
            return false;
        }

        if (VDBG) {
            Log.d(TAG, "writeCharacteristic() - uuid: " + characteristic.getUuid());
        }

        // Add(select write method by the write type)
        int writeType = characteristic.getWriteType();
        switch (writeType) {
            case UsbGattCharacteristic.WRITE_TYPE_DEFAULT:
                writeAttributeRequest(characteristic);
                break;
            case UsbGattCharacteristic.WRITE_TYPE_NO_RESPONSE:
                writeAttributeCommand(characteristic);
                break;
            case UsbGattCharacteristic.WRITE_TYPE_SIGNED:
                break;
            default:
                break;
        }
        return true;
    }


    /**
     * Call this method to write an attribute to the server.
     * <p>Once the server response is received, the {@link WriteAttributeRequestCallback#onWriteSuccess()} callback
     * is invoked, reporting the result of the operation.</p>
     *
     * @param characteristic characteristic to be written to the server.
     */
    private void writeAttributeRequest(UsbGattCharacteristic characteristic) {
        short att_handle = (short) characteristic.getInstanceId();
        byte[] att_value = characteristic.getValue();
        final UsbGattCharacteristic write_characteristic = characteristic;

        WriteAttributeRequest writeRequest = new WriteAttributeRequest(att_handle, att_value);
        writeRequest.addWriteAttributeRequestCallback(new WriteAttributeRequestCallback() {

            @Override
            public void onWriteSuccess() {
                super.onWriteSuccess();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, UsbGatt.GATT_SUCCESS);
                }
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {
                super.onReceiveFailed(att_opcode, request_code, att_handler, error_code);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, getGattErrorCode(error_code));
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, UsbGatt.GATT_FAILURE);
                }
            }
        });
        LocalUsbConnector.getInstance().sendRequest(writeRequest);
    }

    /**
     * Call this method to write an attribute to the server.
     * There have no write response when write a command to server.
     *
     * @param characteristic characteristic to be written to the server.
     */
    private void writeAttributeCommand(UsbGattCharacteristic characteristic) {
        short att_handle = (short) characteristic.getInstanceId();
        byte[] att_value = characteristic.getValue();
        final UsbGattCharacteristic write_characteristic = characteristic;

        WriteAttributeCommand writeCommand = new WriteAttributeCommand(att_handle, att_value);
        writeCommand.addWriteAttributeCommandCallback(new WriteAttributeCommandCallback() {
            @Override
            public void onSendSuccess() {
                super.onSendSuccess();
                if (mUsbGattCallback != null)
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, GATT_SUCCESS);
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null)
                    mUsbGattCallback.onCharacteristicWrite(UsbGatt.this, write_characteristic, GATT_FAILURE);
            }
        });
        LocalUsbConnector.getInstance().writeAttributesCommand(writeCommand);
    }


    /**
     * Convert att error code to gatt error code.
     *
     * @param att_error_code The error code of the att to be converted
     * @return The converted error code of gatt.
     */
    private static int getGattErrorCode(byte att_error_code) {
        int gatt_error_code = att_error_code & 0x0FF;
        switch (gatt_error_code) {
            case GATT_READ_NOT_PERMITTED:
            case GATT_WRITE_NOT_PERMITTED:
            case GATT_INSUFFICIENT_AUTHENTICATION:
            case GATT_REQUEST_NOT_SUPPORTED:
            case GATT_INSUFFICIENT_ENCRYPTION:
            case GATT_INVALID_OFFSET:
            case GATT_INVALID_ATTRIBUTE_LENGTH:
            case GATT_CONNECTION_CONGESTED:
                break;
            default:
                gatt_error_code = GATT_FAILURE;
                break;
        }
        return gatt_error_code;
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
    boolean connect(Context context, UsbGattCallback callback) {
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
        mContext = context;
        mUsbGattCallback = callback;

        int ret = LocalUsbConnector.getInstance().initConnector(mContext);
        if (ret != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "init usb connector failed, error code: " + ret);
            return false;
        }

        ret = LocalUsbConnector.getInstance().setUsbDevice(mDevice);
        if (ret != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "setup usb connector failed, error code: " + ret);
            return false;
        }

        addOnUsbDeviceStatusChangeCallback();

        ret = LocalUsbConnector.getInstance().connect();
        if (ret != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "connect failed, error code: " + ret);
            return false;
        }
        queryBTConnectStateRequest();
        return true;
    }

    /**
     * This callback will trigger when the state of the Bluetooth GATT connection changes.
     */
    private OnUsbDeviceStatusChangeCallback mOnUsbDeviceStatusChangeCallback = new OnUsbDeviceStatusChangeCallback() {
        @Override
        public void onDeviceConnectionStatusHasChanged(boolean connectionStatus) {
            super.onDeviceConnectionStatusHasChanged(connectionStatus);
            if (mUsbGattCallback != null) {
                mUsbGattCallback.onConnectionStateChange(UsbGatt.this, GATT_SUCCESS,
                        connectionStatus ? UsbGatt.STATE_CONNECTED : UsbGatt.STATE_DISCONNECTED);
            }
        }

        @Override
        public void onReceiveHandleValueNotification(short att_handle, byte[] att_value) {
            super.onReceiveHandleValueNotification(att_handle, att_value);
            UsbGattCharacteristic characteristic = new UsbGattCharacteristic(null, att_handle, 0, 0);
            characteristic.setValue(att_value);
            if (mUsbGattCallback != null)
                mUsbGattCallback.onCharacteristicChanged(UsbGatt.this, characteristic);
        }
    };

    private void addOnUsbDeviceStatusChangeCallback() {
        LocalUsbConnector.getInstance().addOnUsbDeviceStatusChangeCallback(mOnUsbDeviceStatusChangeCallback);
    }


    /**
     * Call this method to query the current Bluetooth connection status.
     */
    private void queryBTConnectStateRequest() {
        QueryBTConnectStateRequest queryBTConnectStateRequest = new QueryBTConnectStateRequest();
        queryBTConnectStateRequest.addQueryBTConnectStateRequestCallback(new QueryBTConnectStateRequestCallback() {
            @Override
            public void onReceiveConnectState(int statusCode, int connectState) {
                super.onReceiveConnectState(statusCode, connectState);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onConnectionStateChange(UsbGatt.this, UsbGatt.GATT_SUCCESS, connectState);
                }
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onConnectionStateChange(UsbGatt.this, UsbGatt.GATT_FAILURE, UsbGatt.STATE_DISCONNECTED);
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onConnectionStateChange(UsbGatt.this, UsbGatt.GATT_FAILURE, UsbGatt.STATE_DISCONNECTED);
                }
            }
        });
        LocalUsbConnector.getInstance().sendRequest(queryBTConnectStateRequest);
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
        int initRet = LocalUsbConnector.getInstance().initConnector(mContext);
        if (initRet != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "init usb connector failed, error code: " + initRet);
            return false;
        }

        int setupRet = LocalUsbConnector.getInstance().setUsbDevice(mDevice);
        if (setupRet != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "setup usb connector failed, error code: " + setupRet);
            return false;
        }

        int ret = LocalUsbConnector.getInstance().connect();
        if (ret != UsbError.CODE_NO_ERROR) {
            Log.d(TAG, "connect failed, error code: " + ret);
            return false;
        }
        queryBTConnectStateRequest();
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

        LocalUsbConnector.getInstance().disConnect();
        LocalUsbConnector.getInstance().removeOnUsbDeviceStatusChangeCallback(mOnUsbDeviceStatusChangeCallback);
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

        if (mCharacteristics != null) {
            mCharacteristics.clear();
        }

        // TODO: 2019-12-04
        readDongleConfigRequest();
        return true;
    }

    /**
     * Get all the information of ota Characteristic by this method.
     */
    private void readDongleConfigRequest() {
        ReadDongleConfigRequest readDongleConfigRequest = new ReadDongleConfigRequest();
        readDongleConfigRequest.addReadDongleConfigRequestCallback(new ReadDongleConfigRequestCallback() {
            @Override
            public void onReadOtaCharacteristicList(List<UsbGattCharacteristic> list) {
                super.onReadOtaCharacteristicList(list);
                mCharacteristics = list;
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onServicesDiscovered(UsbGatt.this, UsbGatt.GATT_SUCCESS);
                }
            }

            @Override
            public void onReadFailed() {
                super.onReadFailed();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onServicesDiscovered(UsbGatt.this, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onServicesDiscovered(UsbGatt.this, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onServicesDiscovered(UsbGatt.this, UsbGatt.GATT_FAILURE);
                }
            }
        });
        LocalUsbConnector.getInstance().sendRequest(readDongleConfigRequest);
    }


    /**
     * Call this method to request the server to respond with its maximum receive MTU size.
     * <p>Once the server response is received, the {@link ExchangeMtuRequestCallback#onReceiveServerRxMtu(int)} callback
     * is invoked, reporting the result of the operation.</p>
     *
     * @param mtu client's maximum receive MTU size.
     */
    private void readMtuRequest(int mtu) {
        final int client_mtu_size = mtu;
        ExchangeMtuRequest exchangeMtuRequest = new ExchangeMtuRequest();
        exchangeMtuRequest.addExchangeMtuRequestCallback(new ExchangeMtuRequestCallback() {
            @Override
            public void onReceiveServerRxMtu(int serverMtuSize) {
                super.onReceiveServerRxMtu(serverMtuSize);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onMtuChanged(UsbGatt.this, serverMtuSize, UsbGatt.GATT_SUCCESS);
                }
            }

            @Override
            public void onReceiveFailed() {
                super.onReceiveFailed();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onMtuChanged(UsbGatt.this, client_mtu_size, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onSendFailed(int sendResult) {
                super.onSendFailed(sendResult);
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onMtuChanged(UsbGatt.this, client_mtu_size, UsbGatt.GATT_FAILURE);
                }
            }

            @Override
            public void onReceiveTimeout() {
                super.onReceiveTimeout();
                if (mUsbGattCallback != null) {
                    mUsbGattCallback.onMtuChanged(UsbGatt.this, client_mtu_size, UsbGatt.GATT_FAILURE);
                }
            }
        });
        LocalUsbConnector.getInstance().sendRequest(exchangeMtuRequest);
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
        if (mtu < 0) {
            Log.d(TAG, "request mtu size can not be a negative value.");
            return false;
        }
        readMtuRequest(mtu);
        return true;
    }

}
