package com.realsil.sdk.core.usb;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is use to manager gatt connect, to let all the activity have only a callback.
 *
 * @author bingshanguxue
 */
public class GlobalUsbGatt {
    private boolean D = false;
    private static final String TAG = "GlobalUsbGatt";

    private static GlobalUsbGatt mInstance;

    /**
     * Bluetooth Manager
     */
    private UsbManager mUsbManager;

    /**
     * Pepriphal info
     */
    private List<String> mBdAddrs;
    private HashMap<String, UsbGatt> mUsbGatts;
    private HashMap<String, List<UsbGattCallback>> mCallbacks;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    /**
     * Connection state
     */
    private HashMap<String, Integer> mConnectionState;

    /**
     * for sync gatt callback
     */
    private volatile boolean mGattCallbackCalled;
    /**
     * used for gatt callback
     */
    private final Object mGattCallbackLock = new Object();
    private static final int MAX_CALLBACK_LOCK_WAIT_TIME = 3000;


    private Context mContext;

    /**
     * Constructor. Prepares a new GlobalUsbGatt session.
     *
     * @param context
     */
    private GlobalUsbGatt(Context context) {
        mContext = context;
        mUsbGatts = new HashMap<>();
        mConnectionState = new HashMap<>();
        mCallbacks = new HashMap<>();
        mBdAddrs = new CopyOnWriteArrayList<>();

        initialize();
    }

    public synchronized static void initial(Context context) {
        if (mInstance == null) {
            synchronized (GlobalUsbGatt.class) {
                if (mInstance == null) {
                    mInstance = new GlobalUsbGatt(context.getApplicationContext());
                }
            }
        }
    }

    /**
     * Get the Global gatt object.
     * <p>
     * <p>It will return a instance.
     *
     * @return The GloabalGatt instance.
     */
    public static GlobalUsbGatt getInstance() {
        return mInstance;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mUsbManager == null) {
            mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            if (mUsbManager == null) {
                Log.w(TAG,"USB_SERVICE not supported.");
                return false;
            }
        }

        Log.d(TAG,"initialize success");
        return true;
    }

    public boolean isBluetoothSupported() {
        return mUsbManager != null || initialize();
    }

    /**
     * @param address
     * @return
     */
    public boolean isConnected(String address) {
        Integer state = mConnectionState.get(address);
        if (state == null) {
            return false;
        }
        return (state.equals(STATE_CONNECTED));
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param device  The device address of the destination device.
     * @param callback The gatt callback.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code UsbGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(UsbDevice device, final UsbGattCallback callback) {
        if (mUsbManager == null) {
            Log.w(TAG,"BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (device == null) {
            Log.w(TAG,"Device not found.  Unable to connect.");
            return false;
        }
        if (mBdAddrs.contains(device.getDeviceName())) {
            UsbGatt gatt = mUsbGatts.get(device.getDeviceName());
            // if connect, an other want connect
            if (isConnected(device.getDeviceName())) {
                Log.d(TAG, "already connected, addr=" + device.getDeviceName());
                // register a callback
                registerCallback(device.getDeviceName(), callback);
                // call the connection state change callback to tell it connect
                if (callback != null) {
                    callback.onConnectionStateChange(gatt,
                            UsbGatt.GATT_SUCCESS, UsbGatt.STATE_CONNECTED);
                }
                return true;
            } else {
                // Previously connected device. Try to reconnect.
                if (gatt != null) {
//            close(address);
                    registerCallback(device.getDeviceName(), callback);

                    Log.d(TAG, "re-connect previous device: " + device.getDeviceName());
                    if (gatt.connect()) {
                        mConnectionState.put(device.getDeviceName(), STATE_CONNECTING);
                        if (callback != null) {
                            callback.onConnectionStateChange(gatt,
                                    UsbGatt.GATT_SUCCESS, UsbGatt.STATE_CONNECTING);
                        }
                        return true;
                    } else {
                        Log.w(TAG,"reconnect failed.");
                        closeGatt(device.getDeviceName());
                        return false;
                    }
                }
            }
        }


        // register a callback
        registerCallback(device.getDeviceName(), callback);

        Log.d(TAG, "create connection to " + device.getDeviceName());
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mConnectionState.put(device.getDeviceName(), STATE_CONNECTING);
//        close(address);

        //connecting to the GATT server on the device
        UsbGatt gatt = new UsbGatt(mContext, device);
        gatt.connect(new GattCallback());

        if (gatt == null) {
            Log.w(TAG,"UsbGatt not exist.  Unable to connect.");
        } else {
            mUsbGatts.put(device.getDeviceName(), gatt);
            mBdAddrs.add(device.getDeviceName());
        }
        return true;
    }


    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     * remove callbacks
     *
     * @param addr The device address with want to close gatt
     */
    public synchronized void closeGatt(final String addr) {
        if (addr == null) {
            Log.w(TAG,"Invalid address");
            return;
        }
        Log.d(TAG, "closeGatt， addr:=" + addr);
        if (mUsbGatts != null && mUsbGatts.get(addr) != null) {
            mUsbGatts.get(addr).close();
            mUsbGatts.remove(addr);
        }
        if (mCallbacks != null) {
            mCallbacks.remove(addr);
        }
        if (mBdAddrs != null && mBdAddrs.contains(addr)) {
            mBdAddrs.remove(addr);
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code UsbGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     *
     * @param addr The device address with want to disconnect
     */
    public boolean disconnectGatt(final String addr) {
        UsbGatt bluetoothGatt = mUsbGatts.get(addr);
        List<UsbGattCallback> callbacks = mCallbacks.get(addr);

        if (bluetoothGatt != null) {
            if (isConnected(addr)) {
                Log.d(TAG, "disconnect : " + addr);
                // @2019/11/05 Android 10, when bt off, no gatt callback return
                // E/bt_stack: [ERROR:bta_gattc_act.cc(293)] No such connection need to be cancelled
                // E/bt_stack: [ERROR:bta_gattc_utils.cc(433)] bta_gattc_mark_bg_conn unable to find the bg connection mask for bd_addr=00:89:98:34:43:14
                bluetoothGatt.disconnect();
                // wait 500ms for disconnect, here also can use sync method
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (callbacks != null && callbacks.size() > 0) {
                    for (UsbGattCallback callback : callbacks) {
                        callback.onConnectionStateChange(bluetoothGatt,
                                UsbGatt.GATT_SUCCESS, UsbGatt.STATE_DISCONNECTED);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * When the le services manager close, it must disconnect and close the gatt.
     *
     * @param addr The device address with want to close
     */
    public void close(final String addr) {
        disconnectGatt(addr);
        closeGatt(addr);
    }

    /**
     * Close all the connect device.
     */
    public void closeAll() {
        if (mBdAddrs != null && mBdAddrs.size() > 0) {
            for (String addr : mBdAddrs) {
                close(addr);
            }
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code UsbGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param addr           The device address with want to read
     * @param characteristic The characteristic to read from.
     * @return Return true if the read is initiated successfully. The read result
     * is reported asynchronously through the
     * {@code UsbGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public boolean readCharacteristic(final String addr, UsbGattCharacteristic characteristic) {
        if (mUsbManager == null || mUsbGatts.get(addr) == null) {
            Log.w(TAG,"BluetoothAdapter not initialized or gatt is null");
            return false;
        }
        Log.d(TAG,"raddr: " + addr);
        return mUsbGatts.get(addr).readCharacteristic(characteristic);
//        return true;
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code UsbGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param addr           The device address with want to write
     * @param characteristic The characteristic to write.
     * @return Return true if the write is initiated successfully. The read result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public boolean writeCharacteristic(final String addr, UsbGattCharacteristic characteristic) {
        if (mUsbManager == null || mUsbGatts.get(addr) == null) {
            Log.w(TAG,"BluetoothAdapter not initialized");
            return false;
        }
        Log.d(TAG, "addr: " + addr);
        return mUsbGatts.get(addr).writeCharacteristic(characteristic);
//        return true;
    }


    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}.
     * This is a sync method, only the callback is called, it will return.
     *
     * @param addr           The device address with want to read
     * @param characteristic The characteristic to read from.
     * @return Return true if the read is initiated successfully. The read result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public boolean readCharacteristicSync(final String addr, UsbGattCharacteristic characteristic) {
        mGattCallbackCalled = false;

        if (!readCharacteristic(addr, characteristic)) {
            return false;
        }

        synchronized (mGattCallbackLock) {
            try {
                // here only wait for 3 seconds
                if (!mGattCallbackCalled) {
                    Log.d(TAG, "wait for " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mGattCallbackLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                    Log.d(TAG,"wait time reached");
                }
            } catch (final InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
        return true;
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * This is a sync method, only the callback is called, it will return.
     *
     * @param addr           The device address with want to write
     * @param characteristic The characteristic to write.
     * @return Return true if the write is initiated successfully. The read result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public synchronized boolean writeCharacteristicSync(final String addr, UsbGattCharacteristic characteristic) {
        mGattCallbackCalled = false;

        if (!writeCharacteristic(addr, characteristic)) {
            return false;
        }

        synchronized (mGattCallbackLock) {
            try {
                // here only wait for 3 seconds
                if (!mGattCallbackCalled) {
                    Log.d(TAG,"wait for " + MAX_CALLBACK_LOCK_WAIT_TIME + "ms");
                    mGattCallbackLock.wait(MAX_CALLBACK_LOCK_WAIT_TIME);
                    Log.d(TAG,"wait time reached");
                }
            } catch (final InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
        return true;
    }

    public List<String> getBluetoothDeviceAddresss() {
        return mBdAddrs;
    }


    public UsbGatt getUsbGatt(final String addr) {
        return mUsbGatts.get(addr);
    }

    public ArrayList<UsbDevice> getConnectDevices() {
        ArrayList<UsbDevice> devices = new ArrayList<>();
        for (String addr : mBdAddrs) {
            if (isConnected(addr)) {
                devices.add(getUsbGatt(addr).getDevice());
            }
        }
        return devices;
    }

    public String getDeviceName(final String addr) {
        UsbGatt bluetoothGatt = mUsbGatts.get(addr);
        if (bluetoothGatt == null) {
            Log.w(TAG,"no bluetoothGatt exist, addr=" + addr);
            return null;
        }
        return bluetoothGatt.getDevice().getDeviceName();
    }


    /**
     * Implements callback methods for GATT events that the app cares about.  For example,
     * connection change and services discovered.
     */
    private class GattCallback extends UsbGattCallback {


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(UsbGatt gatt, int mtu, int status) {
            String addr = gatt.getDevice().getDeviceName();
            if (D) {
                Log.d(TAG, String.format(Locale.US, "%d << mtu= %d, addr=%s",
                        status, mtu, addr));
            }

            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onMtuChanged(gatt, mtu, status);
                }
            }
        }

        @Override
        public void onConnectionStateChange(UsbGatt gatt, int status, int newState) {
            UsbDevice device = gatt.getDevice();
            if (device == null) {
                return;
            }

            String addr = device.getDeviceName();
            if (D) {
                Log.d(TAG,String.format(Locale.US, "%s, status: %d , newState: %d",
                        addr, status, newState));
            }

            if (status == UsbGatt.GATT_SUCCESS) {
                if (newState == UsbGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server.");
                    mConnectionState.put(addr, STATE_CONNECTED);
                    mUsbGatts.put(addr, gatt);
                } else {
                    Log.d(TAG, "Disconnected from GATT server.");
                    mConnectionState.put(addr, STATE_DISCONNECTED);
//                    closeBluetoothGatt(addr);//prevent 133 error
                }
            } else {
                // TODO: 23/05/2018  
                mConnectionState.put(addr, STATE_DISCONNECTED);
//                closeBluetoothGatt(addr);//prevent 133 error
            }

            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onConnectionStateChange(gatt, status, newState);
                }
            }
        }

        @Override
        public void onServicesDiscovered(UsbGatt gatt, int status) {
            String addr = gatt.getDevice().getDeviceName();
            if (D) {
                Log.d(TAG,String.format(Locale.US, "%d << addr=%s",
                        status, addr));
            }

            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onServicesDiscovered(gatt, status);
                }
            }
        }

        @Override
        public void onCharacteristicRead(UsbGatt gatt, UsbGattCharacteristic characteristic, int status) {
            byte[] value = characteristic.getValue();
            if (D) {
                if (value != null) {
                    Log.d(TAG,String.format(Locale.US, "%d << %s\n:\t(%d)%s",
                            status, characteristic.getUuid(), value.length, Arrays.toString(value)));
                }
            }

            // notify waiting thread
            synchronized (mGattCallbackLock) {
                mGattCallbackCalled = true;
                mGattCallbackLock.notifyAll();
            }

            String addr = gatt.getDevice().getDeviceName();
            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onCharacteristicRead(gatt, characteristic, status);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(UsbGatt gatt, UsbGattCharacteristic characteristic) {
            String addr = gatt.getDevice().getDeviceName();
            byte[] value = characteristic.getValue();
            if (D) {
                if (value != null) {
                    Log.d(TAG,String.format(Locale.US, "<< %s\n(%d)%s",
                            characteristic.getUuid(), value.length, Arrays.toString(value)));
                } else {
                    Log.d(TAG,String.format(Locale.US, "<< %s", characteristic.getUuid()));
                }
            }

            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onCharacteristicChanged(gatt, characteristic);
                }
            }
        }


        @Override
        public void onCharacteristicWrite(final UsbGatt gatt, final UsbGattCharacteristic characteristic, final int status) {
            String addr = gatt.getDevice().getDeviceName();
            byte[] value = characteristic.getValue();
            if (D) {
                if (value != null) {
                    Log.d(TAG,String.format(Locale.US, "%d << %s\n(%d)%s", status,
                            characteristic.getUuid(), value.length, Arrays.toString(value)));
                }
            }

            // notify waiting thread
            synchronized (mGattCallbackLock) {
                mGattCallbackCalled = true;
                mGattCallbackLock.notifyAll();
            }
            List<UsbGattCallback> callbacks = mCallbacks.get(addr);
            if (callbacks != null && callbacks.size() > 0) {
                for (UsbGattCallback callback : callbacks) {
                    callback.onCharacteristicWrite(gatt, characteristic, status);
                }
            }
        }
    }

    public boolean isCallbackRegisted(String addr, UsbGattCallback callback) {
        List<UsbGattCallback> callbacks = getCallback(addr);
        return callbacks != null && callbacks.contains(callback);
    }

    public List<UsbGattCallback> getCallback(final String addr) {
        return mCallbacks != null ? mCallbacks.get(addr) : null;
    }

    public UsbGatt getBluetoothGatt(final String addr) {
        return mUsbGatts.get(addr);
    }

    /**
     * register a callback
     */
    public void registerCallback(final String addr, UsbGattCallback callback) {
        List<UsbGattCallback> callbacks = getCallback(addr);
        if (callbacks == null) {
            callbacks = new CopyOnWriteArrayList<>();
            callbacks.add(callback);
            mCallbacks.put(addr, callbacks);
        } else if (!(callbacks.contains(callback))) {
            // register a callback
            callbacks.add(callback);
            mCallbacks.put(addr, callbacks);
        }

        Log.d(TAG, "addr: " + addr + ", size = " + callbacks.size());
    }

    public void unRegisterCallback(final String addr, UsbGattCallback callback) {
        List<UsbGattCallback> callbacks = getCallback(addr);
        if (callbacks == null) {
            Log.d(TAG,"callback not registered, addr= " + addr);
            return;
        }

        // unregister a callback
        if (callbacks.contains(callback)) {
            Log.d(TAG, "unregister a callback, addr= " + addr);
            // unregister a callback
            callbacks.remove(callback);
            mCallbacks.put(addr, callbacks);
        }
    }

    public void unRegisterAllCallback(final String addr) {
        if (mCallbacks.get(addr) == null) {
            Log.w(TAG, "mCallbacks.get(addr) == null");
            return;
        }
        Log.d(TAG,"addr: " + addr);
        mCallbacks.remove(addr);
    }
}
