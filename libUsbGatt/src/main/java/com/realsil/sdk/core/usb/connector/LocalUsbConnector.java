package com.realsil.sdk.core.usb.connector;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.callback.OnReceiveServerIndicationCallback;
import com.realsil.sdk.core.usb.connector.att.callback.OnReceiveServerNotificationCallback;
import com.realsil.sdk.core.usb.connector.att.callback.ReadAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeCommandCallback;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;
import com.realsil.sdk.core.usb.connector.callback.OnUsbDeviceStatusChangeCallback;
import com.realsil.sdk.core.usb.connector.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LocalUsbConnector {

    private static final String TAG = "xp.chen";

    private UsbManager mUsbManager;

    /**
     * Usb device currently associated with android device
     */
    private UsbDevice mSelectUsbDevice;

    private UsbEndpoint mUsbEndpointBulkIn;
    private UsbEndpoint mUsbEndpointBulkOut;
    private UsbEndpoint mUsbEndpointControlIn;
    private UsbEndpoint mUsbEndpointControlOut;
    private UsbEndpoint mUsbEndpointInterruptIn;
    private UsbEndpoint mUsbEndpointInterruptOut;

    private UsbInterface mUsbInterfaceBulkIn;
    private UsbInterface mUsbInterfaceBulkOut;
    private UsbInterface mUsbInterfaceControlIn;
    private UsbInterface mUsbInterfaceControlOut;
    private UsbInterface mUsbInterfaceInterruptIn;
    private UsbInterface mUsbInterfaceInterruptOut;

    /**
     * This class is used for sending and receiving data and control messages to a USB device.
     */
    private UsbDeviceConnection mUsbDeviceConnection;

    private Context mContext = null;

    private static final int BULK_TRANSFER_SEND_MAX_TIMEOUT    = 5000;
    private static final int BULK_TRANSFER_RECEIVE_MAX_TIMEOUT = 10 * 1000;

    /**
     * Set the maximum buffer size for data received on interrupt out endpoint.
     */
    private static final int RECEIVE_BUFF_SIZE_ON_INTERRUPT_OUT = 255;


    /* Global lock and condition */
    private final ReentrantLock mSendNextRequestLock                = new ReentrantLock();
    private final ReentrantLock mWriteData2BulkOutEndpointLock      = new ReentrantLock();
    private final ReentrantLock mWriteData2InterruptOutEndpointLock = new ReentrantLock();

    private final Condition mSendNextRequestCondition = mSendNextRequestLock.newCondition();
    /* Global lock and condition */

    private CopyOnWriteArrayList<OnReceiveServerNotificationCallback> mServerNotificationCallbacks;
    private CopyOnWriteArrayList<OnReceiveServerIndicationCallback>   mServerIndicationCallbacks;
    private CopyOnWriteArrayList<OnUsbDeviceStatusChangeCallback>     mOnUsbDeviceStatusChangeCallbacks;

    /**
     * Maximum response time when send a request to server, in second.
     * <p>A transaction not completed within 30 seconds shall time out. Such a transaction
     * shall be considered to have failed and the local higher layers shall be informed of this
     * failure. No more attribute protocol requests, commands, indications or notifications
     * shall be sent to the target device on this ATT Bearer </p>
     */
    private static final int MAXIMUM_RESPONSE_TIME_WHEN_SEND_REQUEST = 30;

    /**
     * Record the request currently being sent. Only one request message can be sent
     * at a time. You must wait for the corresponding response from server before sending the next request.
     */
    private BaseRequest mSendingRequest;

    /* Send write attribute command thread pool args */
    /**
     * Core thread num of send write command thread pool
     */
    private static final int CORE_THREAD_NUM_SEND_WRITE_COMMAND = 10;
    /**
     * Max thread num of send write command thread pool
     */
    private static final int MAX_THREAD_NUM_SEND_WRITE_COMMAND  = 10;
    /**
     * Keep alive time of idle thread in send write command thread pool
     */
    private static final int KEEP_ALIVE_TIME_SEND_WRITE_COMMAND = 1000;

    /**
     * Thread pool for executing send write command tasks
     */
    private ThreadPoolExecutor mSendWriteCommandExecutor;
    /* Send write attribute command thread pool args */

    /**
     * A thread handle. The task of this thread is to continuously obtain a Write Request
     * from the cache queue, and then send it to the usb bulk out endpoint. Sending a new
     * write request message requires receiving the write response corresponding to the previous write request.
     *
     * @see LocalUsbConnector#startReceivingRequestData()
     */
    private Thread mSendRequestThread;

    /**
     * A thread handle whose main task is to listen to data from the usb bulk in endpoint.
     *
     * @see UsbConstants#USB_ENDPOINT_XFER_BULK
     * @see UsbConstants#USB_DIR_IN
     * @see LocalUsbConnector#startListenBulkInData()
     */
    private ListenUsbBulkInDataThread mListenUsbBulkInDataThread;

    /**
     * A thread handle whose main task is to listen to data from the usb interrupt in endpoint.
     *
     * @see UsbConstants#USB_ENDPOINT_XFER_INT
     * @see UsbConstants#USB_DIR_IN
     * @see LocalUsbConnector#startListenInterruptInData()
     */
    private ListenUsbInterruptInDataThread mListenUsbInterruptInDataThread;

    private static volatile LocalUsbConnector instance = null;

    private LocalUsbConnector() {}

    public static LocalUsbConnector getInstance() {
        if (instance == null) {
            synchronized (LocalUsbConnector.class) {
                if (instance == null) instance = new LocalUsbConnector();
            }
        }
        return instance;
    }

    /**
     * Call this method to initialize the Usb connector.
     *
     * @param context Application context
     * @return Results of initialization
     */
    public int initConnector(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
            mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            if (mUsbManager == null) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "can not get usbManager"));
                return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
            }
            // Add receive to listen connection process.
            initUsbReceiver();
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "context parameter can not be null"));
            return UsbError.CODE_PARAMS_IS_NULL;
        }
        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Find the specified USB device based on vendorId and productId.
     *
     * @param vendorId  The vendorId of specified USB device
     * @param productId The productId of specified USB device
     * @return Find result, {@link UsbError#CODE_NO_ERROR} for success, or negative value for failure
     * @see UsbError
     */
    public int searchUsbDevice(int vendorId, int productId) {
        if (mUsbManager == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "search failed, can not get usbManager"));
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "search failed, can not found usb device"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_DEVICE;
        }

        for (UsbDevice usbDevice : deviceList.values()) {
            if (vendorId == 0 && productId == 0 && usbDevice != null) {
                mSelectUsbDevice = usbDevice;
                break;
            }

            if (usbDevice != null) {
                int vid = usbDevice.getVendorId();
                int pid = usbDevice.getProductId();
                if (vid == vendorId && pid == productId) {
                    mSelectUsbDevice = usbDevice;
                    break;
                }
            }
        }

        if (mSelectUsbDevice == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR,
                    "search failed, can not found specified usb device, vid: " + vendorId + ", pid: " + productId));
            return UsbError.CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
        }

        Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "found the specified usb device"));
        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Find USB device.
     *
     * @return Find result, {@link UsbError#CODE_NO_ERROR} for success, or negative value for failure.
     * @see UsbError
     */
    public int searchUsbDevice() {
        return searchUsbDevice(0, 0);
    }

    /**
     * Authorize the found USB device, When this method is called, the interface will pop up
     * an authorization dialog to remind the user to authorize.
     *
     * @return Authorize result. {@link UsbError#CODE_NO_ERROR} for success, or negative value for failure.
     */
    public int authorizeDevice() {
        if (mUsbManager == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "authorize failed, can not get usbManager"));
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        if (mSelectUsbDevice == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "authorize failed, can not found specified usb device"));
            return UsbError.CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
        }

        // Check the permission of current Usb device
        if (!mUsbManager.hasPermission(mSelectUsbDevice)) {
            PendingIntent requestUsbIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(UsbAction.ACTION_REQUEST_USB_PERMISSION), 0);
            mUsbManager.requestPermission(mSelectUsbDevice, requestUsbIntent);
        } else {
            printAuthorizedDeviceInfo();
            notifyDeviceHasAuthorized(true);
        }
        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Call this method to pass a specified {@link UsbDevice} parameter to establish a usb connection.
     *
     * @param usbDevice {@link UsbDevice} object specified by the user.
     * @return result of usb connection establishment, if the usb connection is established successfully, it will be {@link UsbError#CODE_NO_ERROR},
     * otherwise it will be one of those {@link UsbError} values.
     */
    public int setUsbDevice(UsbDevice usbDevice) {
        if (usbDevice != null) {
            this.mSelectUsbDevice = usbDevice;
            return setupDevice();
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "set usb device failed, params can not be null"));
            return UsbError.CODE_PARAMS_IS_NULL;
        }
    }

    /**
     * Configure the selected usb device, the configured device must be authorized. Before calling this method,
     * please call {@link LocalUsbConnector#authorizeDevice()}method to complete usb device authorization.
     *
     * <p><br>Note:</br></p>
     * <p>If you get a {@link UsbDevice} object by calling {@link LocalUsbConnector#searchUsbDevice()}
     * or {@link LocalUsbConnector#searchUsbDevice(int, int)} method , you will not need to
     * call {@link LocalUsbConnector#setUsbDevice(UsbDevice)} again, you just need to call
     * {@link LocalUsbConnector#authorizeDevice()} method to ensure that the currently connected usb device
     * is authorized.</p>
     *
     * <p>If you pass a {@link UsbDevice} object to the {@link LocalUsbConnector} by
     * the {@link LocalUsbConnector#setUsbDevice(UsbDevice)}, you only need to ensure that the passed {@link UsbDevice}
     * object is authorized, you don't need to call current method.</p>
     *
     * @return Configure Result, {@link UsbError#CODE_NO_ERROR} for success, or negative value for failure.
     * @see LocalUsbConnector#authorizeDevice()
     */
    public int setupDevice() {
        if (mUsbManager == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not get usbManager"));
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        if (mSelectUsbDevice == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found specified usb device"));
            return UsbError.CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
        }

        if (!mUsbManager.hasPermission(mSelectUsbDevice)) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, device has not been authorize"));
            return UsbError.CODE_DEVICE_IS_NOT_AUTHORIZED;
        }

        // Clear existing endpoints and interfaces(if they exist)
        mUsbEndpointBulkIn = null;
        mUsbEndpointBulkOut = null;
        mUsbEndpointInterruptIn = null;
        mUsbEndpointInterruptOut = null;

        mUsbInterfaceBulkIn = null;
        mUsbInterfaceBulkOut = null;
        mUsbInterfaceInterruptIn = null;
        mUsbInterfaceInterruptOut = null;

        for (int i = 0; i < mSelectUsbDevice.getInterfaceCount(); i++) {
            UsbInterface usbInterface = mSelectUsbDevice.getInterface(i);
            /*if ((UsbConstants.USB_CLASS_AUDIO != usbInterface.getInterfaceClass())
                    && (UsbConstants.USB_CLASS_HID != usbInterface.getInterfaceClass())) {
                // Filter only specified types of devices

                continue;
            }*/

            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                // Find Bulk Endpoint
                if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        mUsbEndpointBulkIn = usbEndpoint;
                        mUsbInterfaceBulkIn = usbInterface;
                    } else {
                        mUsbEndpointBulkOut = usbEndpoint;
                        mUsbInterfaceBulkOut = usbInterface;
                    }
                }

                // Control transmission
                /*if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        mUsbEndpointControlIn = usbEndpoint;
                        mUsbInterfaceControlIn = usbInterface;
                    } else {
                        mUsbEndpointControlOut = usbEndpoint;
                        mUsbInterfaceControlOut = usbInterface;
                    }
                }*/

                // Find Interrupt endpoint
                if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        mUsbEndpointInterruptIn = usbEndpoint;
                        mUsbInterfaceInterruptIn = usbInterface;
                    } else {
                        mUsbEndpointInterruptOut = usbEndpoint;
                        mUsbInterfaceInterruptOut = usbInterface;
                    }
                }
            }
        }

        /* check bulk in interface & endpoint */
        // interface
        /*if (mUsbInterfaceBulkIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk in interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }
        // endpoint
        if (mUsbEndpointBulkIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }*/

        /* check interrupt in interface & endpoint */
        // interface
        /*if (mUsbInterfaceInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb interrupt in interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }
        // endpoint
        if (mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb interrupt in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }*/

        /* check bulk out interface & endpoint */
        // interface
        /*if (mUsbInterfaceBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk out interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }
        // endpoint
        if (mUsbEndpointBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk out endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }*/

        // Check input endpoints, one of interrupt in and bulk in must be present
        if (mUsbEndpointBulkIn == null && mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb input endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "The required endpoint has been found"));

        // Open Usb Connection
        mUsbDeviceConnection = mUsbManager.openDevice(mSelectUsbDevice);
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not open the usb connection"));
            return UsbError.CODE_OPEN_USB_CONNECTION_FAILED;
        }

        // claim bulk out interface
        if (mUsbInterfaceBulkOut != null) {
            boolean holdBulkOutRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkOut, true);
            if (!holdBulkOutRet) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim bulk out interface failed"));
                return UsbError.CODE_HOLD_USB_INTERFACE;
            }
        }

        // claim bulk in interface
        if (mUsbInterfaceBulkIn != null) { // Listen if this interface exists
            boolean holdBulkInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkIn, true);
            if (!holdBulkInRet) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim bulk in interface failed"));
                return UsbError.CODE_HOLD_USB_INTERFACE;
            }
        }

        // claim interrupt out interface
        if (mUsbInterfaceInterruptOut != null) {
            boolean holdInterruptOutRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceInterruptOut, true);
            if (!holdInterruptOutRet) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim interrupt out interface failed"));
                return UsbError.CODE_HOLD_USB_INTERFACE;
            }
        }

        // claim interrupt in interface
        if (mUsbInterfaceInterruptIn != null) { // Listen if this interface exists
            boolean holdInterruptInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceInterruptIn, true);
            if (!holdInterruptInRet) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim interrupt in interface failed"));
                return UsbError.CODE_HOLD_USB_INTERFACE;
            }
        }

        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Call this method to print device info which has been authorized.
     */
    private void printAuthorizedDeviceInfo() {
        if (mSelectUsbDevice != null) {
            // Print current usb detail info.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "authorize success, Device Name: "
                        + mSelectUsbDevice.getDeviceName()
                        + "Product Name: " + mSelectUsbDevice.getProductName()
                        + "Serial Number: " + mSelectUsbDevice.getSerialNumber()));
            } else {
                Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "authorize success, Device Name: " + mSelectUsbDevice.getDeviceName()));
            }
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "authorize failed, device has not been authorize"));
        }
    }

    /**
     * Notify all callbacks that the current device is authorized.
     */
    private void notifyDeviceHasAuthorized(boolean authorizeResult) {
        if (mOnUsbDeviceStatusChangeCallbacks != null) {
            for (OnUsbDeviceStatusChangeCallback callback : mOnUsbDeviceStatusChangeCallbacks) {
                callback.authorizeCurrentDevice(authorizeResult);
            }
        }
    }

    private void notifyDeviceStatusChange(int errorCode, String detailInfo) {
        if (mOnUsbDeviceStatusChangeCallbacks != null) {
            for (OnUsbDeviceStatusChangeCallback callback : mOnUsbDeviceStatusChangeCallbacks) {
                callback.onDeviceStatusChange(errorCode, detailInfo);
            }
        }
    }

    private void initUsbReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbAction.ACTION_REQUEST_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private void destroyUsbReceiver() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (Objects.equals(intent.getAction(), UsbAction.ACTION_REQUEST_USB_PERMISSION)) {
                // Handling user authorization results
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (device != null && granted) {
                    mSelectUsbDevice = device;
                    printAuthorizedDeviceInfo();
                    notifyDeviceHasAuthorized(true);
                } else {
                    mSelectUsbDevice = null;
                    notifyDeviceHasAuthorized(false);
                }
            } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "device has detached, need to re-establish connection"));
                disConnect();
            }

        }
    };


    /**
     * Call this method to add a callback for listening notification messages from server.
     *
     * @param callback A callback interface for listening notification message from the server.
     * @see OnReceiveServerNotificationCallback
     */
    public void addOnServerNotificationCallback(OnReceiveServerNotificationCallback callback) {
        if (callback == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "callback parameter can not be null"));
            return;
        }

        if (mServerNotificationCallbacks == null) {
            mServerNotificationCallbacks = new CopyOnWriteArrayList<>();
            mServerNotificationCallbacks.add(callback);
        } else {
            if (!mServerNotificationCallbacks.contains(callback)) {
                mServerNotificationCallbacks.add(callback);
            }
        }
    }

    /**
     * Call this method to remove a registered notification callback.
     *
     * @param callback Callback to be removed
     */
    public void removeOnServerNotificationCallback(OnReceiveServerNotificationCallback callback) {
        if (mServerNotificationCallbacks != null) {
            mServerNotificationCallbacks.remove(callback);
        }
    }

    /**
     * Call this method to add a callback for listening indication messages from server.
     *
     * @param callback A callback interface for listening indication message from the server.
     * @see OnReceiveServerIndicationCallback
     */
    public void addOnServerIndicationCallback(OnReceiveServerIndicationCallback callback) {
        if (callback == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "callback parameter can not be null"));
            return;
        }

        if (mServerIndicationCallbacks == null) {
            mServerIndicationCallbacks = new CopyOnWriteArrayList<>();
            mServerIndicationCallbacks.add(callback);
        } else {
            if (!mServerIndicationCallbacks.contains(callback)) {
                mServerIndicationCallbacks.add(callback);
            }
        }
    }

    /**
     * Call this method to remove a registered indication callback.
     *
     * @param callback Callback to be removed
     */
    public void removeOnServerIndicationCallback(OnReceiveServerIndicationCallback callback) {
        if (mServerIndicationCallbacks != null) {
            mServerIndicationCallbacks.remove(callback);
        }
    }

    /**
     * Call this method to add a callback for listening usb device status.
     *
     * @param onUsbDeviceStatusChangeCallback A callback interface for listening usb device status.
     */
    public void addOnUsbDeviceStatusChangeCallback(OnUsbDeviceStatusChangeCallback onUsbDeviceStatusChangeCallback) {
        if (onUsbDeviceStatusChangeCallback == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "onUsbDeviceStatusChangeCallback parameter can not be null"));
            return;
        }

        if (mOnUsbDeviceStatusChangeCallbacks == null) {
            mOnUsbDeviceStatusChangeCallbacks = new CopyOnWriteArrayList<>();
            mOnUsbDeviceStatusChangeCallbacks.add(onUsbDeviceStatusChangeCallback);
        } else {
            if (!mOnUsbDeviceStatusChangeCallbacks.contains(onUsbDeviceStatusChangeCallback)) {
                mOnUsbDeviceStatusChangeCallbacks.add(onUsbDeviceStatusChangeCallback);
            }
        }
    }

    /**
     * Call this method to remove a UsbDeviceStatusChangeCallback
     *
     * @param onUsbDeviceStatusChangeCallback A callback interface for listening usb device status.
     */
    public void removeOnUsbDeviceStatusChangeCallback(OnUsbDeviceStatusChangeCallback onUsbDeviceStatusChangeCallback) {
        if (mOnUsbDeviceStatusChangeCallbacks != null) {
            mOnUsbDeviceStatusChangeCallbacks.remove(onUsbDeviceStatusChangeCallback);
        }
    }


    /**
     * Start new Thread to listen for data coming from the bulk in endpoint.
     *
     * @see UsbConstants#USB_ENDPOINT_XFER_BULK
     * @see UsbConstants#USB_DIR_IN
     */
    private void startListenBulkInData() {
        if (mListenUsbBulkInDataThread == null) {
            mListenUsbBulkInDataThread = new ListenUsbBulkInDataThread();
            mListenUsbBulkInDataThread.start();
        }
    }

    /**
     * Stop to listen for data coming from the bulk in endpoint.
     */
    private void stopListenBulkInData() {
        if (mListenUsbBulkInDataThread != null) {
            mListenUsbBulkInDataThread.interrupt();
            mListenUsbBulkInDataThread = null;
        }
    }

    /**
     * Start new Thread to listen for data coming from the interrupt in endpoint.
     *
     * @see UsbConstants#USB_ENDPOINT_XFER_INT
     * @see UsbConstants#USB_DIR_IN
     */
    private void startListenInterruptInData() {
        if (mListenUsbInterruptInDataThread == null) {
            mListenUsbInterruptInDataThread = new ListenUsbInterruptInDataThread();
            mListenUsbInterruptInDataThread.start();
        }
    }

    /**
     * Stop to listen for data coming from the interrupt in endpoint.
     */
    private void stopListenInterruptInData() {
        if (mListenUsbInterruptInDataThread != null) {
            mListenUsbInterruptInDataThread.interrupt();
            mListenUsbInterruptInDataThread = null;
        }
    }

    /**
     * Listen for data from the usb bulk in endpoint
     */
    private class ListenUsbBulkInDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "start listening for bulk in endpoint data..."));
            while (!isInterrupted()) {
                byte[] recvBuf = new byte[mUsbEndpointBulkIn.getMaxPacketSize()];
                int recvlen = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkIn, recvBuf, recvBuf.length, 0);
                if (recvlen > 0) {
                    // Parse receive data
                    byte[] recvData = new byte[recvlen];
                    System.arraycopy(recvBuf, 0, recvData, 0, recvlen);
                    Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS,
                            "receive data (bulk in, len = " + recvlen + "): " + ByteUtil.convertHexString(recvData)));
                    parseReceiveData(recvData);
                } else {
                    Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "receive data failed, " + recvlen));
                }
            }
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "interrupt bulk in listening thread"));
        }
    }

    /**
     * Listen for data from the usb interrupt in endpoint
     */
    private class ListenUsbInterruptInDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "start listening for interrupt in endpoint data..."));
            while (!isInterrupted()) {
                int recvMaxSize = mUsbEndpointInterruptIn.getMaxPacketSize();
                ByteBuffer buffer = ByteBuffer.allocate(recvMaxSize);
                UsbRequest usbRequest = new UsbRequest();
                usbRequest.initialize(mUsbDeviceConnection, mUsbEndpointInterruptIn);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    usbRequest.queue(buffer);
                } else {
                    usbRequest.queue(buffer, recvMaxSize);
                }
                if (mUsbDeviceConnection.requestWait() == usbRequest) {
                    byte[] recvData = buffer.array();
                    Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS,
                            "receive data (interrupt in, len = " + recvData.length + "): " + ByteUtil.convertHexString(recvData)));
                    parseReceiveData(recvData);
                }
            }
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "interrupt interrupt in listening thread"));
        }
    }

    /**
     * According to the opcode returned by the usb, call the corresponding parsing
     * method to parse the data returned by the usb.
     *
     * @param receiveData Data returned by the server.
     */
    private void parseReceiveData(byte[] receiveData) {
        byte reportID = receiveData[0];
        switch (reportID) {
            case UsbConfig.REPORT_ID_16:
            case UsbConfig.REPORT_ID_17:
            case UsbConfig.REPORT_ID_18:
            case UsbConfig.REPORT_ID_19:
                parseAttPduResponseData(receiveData);
                break;
            case UsbConfig.REPORT_ID_4: // Usb Management
                parseUsbCmdResponseData(receiveData);
                break;
            default:
                break;
        }
    }

    /**
     * Call this method to parse the received response from server after send a request.
     *
     * @param responseData received response data from server.
     */
    private void parseAttPduResponseData(byte[] responseData) {
        byte responseOpcode = responseData[2];
        switch (responseOpcode) {
            case AttPduOpcodeDefine.WRITE_RESPONSE:
            case AttPduOpcodeDefine.READ_RESPONSE:
            case AttPduOpcodeDefine.EXCHANGE_MTU_RESPONSE:
                if (mSendingRequest != null) {
                    String logInfoType = mSendingRequest.getClass().getSimpleName();
                    Log.i(TAG, UsbLogInfo.msg(logInfoType, "has received a server response"));
                    mSendingRequest.parseResponse(responseData);
                    // int parseResult = mSendingRequest.getParseResult();
                } else {
                    Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "parse att pdu failed, Internal status exception"));
                }
                break;
            case AttPduOpcodeDefine.HANDLE_VALUE_INDICATION:
                parseIndicationMessageFromServer(responseData);
                break;
            case AttPduOpcodeDefine.HANDLE_VALUE_NOTIFICATION:
                parseNotificationMessageFromServer(responseData);
                break;
            default:
                break;
        }
    }

    private void parseUsbCmdResponseData(byte[] responseData) {
        if (mSendingRequest != null) {
            String logInfoType = mSendingRequest.getClass().getSimpleName();
            Log.i(TAG, UsbLogInfo.msg(logInfoType, "has received a server response"));
            mSendingRequest.parseResponse(responseData);
            // int parseResult = mSendingRequest.getParseResult();
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "parse usb cmd failed, Internal status exception"));
        }
    }

    private void parseIndicationMessageFromServer(byte[] indicationData) {
        if (mServerIndicationCallbacks != null) {
            for (OnReceiveServerIndicationCallback callback : mServerIndicationCallbacks) {
                callback.onReceiveServerIndication(indicationData);
            }
        }
    }

    private void parseNotificationMessageFromServer(byte[] notificationData) {
        if (mServerNotificationCallbacks != null) {
            for (OnReceiveServerNotificationCallback callback : mServerNotificationCallbacks) {
                callback.onReceiveServerNotification(notificationData);
            }
        }
    }

    /**
     * Calling this method will create a cache queue to store the request that the user
     * will send, and start a thread to send the request in cache queue.
     * <p>request may be one of read request or write request</p>
     */
    private void startReceivingRequestData() {
        if (mSendRequestCacheQueue == null) {
            // Create a new cache queue for storing request.
            mSendRequestCacheQueue = new LinkedBlockingQueue<>();
        }

        if (mSendRequestThread == null) {
            mSendRequestThread = new SendRequestThread();
            mSendRequestThread.start();
        }
    }

    private void stopReceivingRequestData() {
        if (mSendRequestThread != null) {
            mSendRequestThread.interrupt();
            mSendRequestThread = null;
        }

        if (mSendRequestCacheQueue != null) {
            mSendRequestCacheQueue.clear();
            mSendRequestCacheQueue = null;
        }
    }


    /**
     * Calling this method will create a cache queue for sending write command messages.
     */
    private void startReceivingWriteCommandData() {
        if (mSendWriteCommandExecutor == null) {
            LinkedBlockingQueue<Runnable> writeAttributeCommandCacheQueue = new LinkedBlockingQueue<Runnable>();
            mSendWriteCommandExecutor = new ThreadPoolExecutor(
                    CORE_THREAD_NUM_SEND_WRITE_COMMAND,
                    MAX_THREAD_NUM_SEND_WRITE_COMMAND,
                    KEEP_ALIVE_TIME_SEND_WRITE_COMMAND,
                    TimeUnit.MILLISECONDS,
                    writeAttributeCommandCacheQueue,
                    new ThreadPoolExecutor.AbortPolicy()
            );
        }
    }

    private void stopReceivingWriteCommandData() {
        if (mSendWriteCommandExecutor != null) {
            mSendWriteCommandExecutor.shutdown();
            mSendWriteCommandExecutor = null;
        }
    }

    /**
     * Call this method to send a request instance to usb.
     * <p>You can add a callback object on the object to listen the execution status of this instruction</p>
     *
     * <p> For example, You can add a callback method {@link WriteAttributeRequest#addWriteAttributeRequestCallback(WriteAttributeRequestCallback)} on
     * the {@link WriteAttributeRequest} object to monitor the data write status</p>
     *
     * @see WriteAttributeRequest#addWriteAttributeRequestCallback(WriteAttributeRequestCallback)
     * @see ReadAttributeRequest#addReadAttributeRequestCallback(ReadAttributeRequestCallback)
     */
    public void sendRequest(BaseRequest requestInstance) {
        if (requestInstance == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "send failed, sent request can not be null"));
            return;
        }

        if (mSendRequestCacheQueue != null) {
            mSendRequestCacheQueue.offer(requestInstance);
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "send failed, connection has not been established"));
        }
    }

    /**
     * Call this method to write a attribute value (typically into a control-point attribute) to the server.
     * <p>Note: No Error Response or Write Response shall be sent in response to this
     * command. If the server cannot write this attribute for any reason the command
     * shall be ignored.</p>
     *
     * @param writeAttributesCommand An entity object that encapsulates some related information of the Attribute
     * @see WriteAttributeCommand
     */
    public void writeAttributesCommand(WriteAttributeCommand writeAttributesCommand) {
        if (writeAttributesCommand == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_COMMAND, "send failed, argus can not be null"));
            return;
        }

        if (mSendWriteCommandExecutor != null) {
            WriteAttributesCommandRunnable runnable = new WriteAttributesCommandRunnable(writeAttributesCommand);
            mSendWriteCommandExecutor.execute(runnable);
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_COMMAND, "send failed, connection has not been established"));
        }
    }


    /**
     * A blocked buffer queue for storing request messages.
     * <p>When the usb connection is disconnected, this queue needs to be cleared.</p>
     */
    private LinkedBlockingQueue<BaseRequest> mSendRequestCacheQueue;

    /**
     * Call this method to write data to the bulk out endpoint of the USB.
     *
     * @param writeData Data to be written.
     * @return Written result. length of data transferred (or zero) for success, or negative value for failure
     * @see UsbError#CODE_USB_CONNECTION_NOT_ESTABLISHED
     * @see UsbError#CODE_USB_SEND_DATA_FAILED
     */
    private int writeData2BulkOutEndpoint(byte[] writeData) {
        // TODO: 2020/1/2 Comment this code for now, audio data will be sent on this endpoint in the future.
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "write bulk out failed, connection has not been established"));
            return UsbError.CODE_USB_CONNECTION_NOT_ESTABLISHED;
        }
        mWriteData2BulkOutEndpointLock.lock();
        int writeRet = -1;
        try {
            writeRet = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkOut, writeData, writeData.length, BULK_TRANSFER_SEND_MAX_TIMEOUT);
            if (writeRet < 0) return UsbError.CODE_USB_SEND_DATA_FAILED;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWriteData2BulkOutEndpointLock.unlock();
        }
        return writeRet;
    }


    private class writeRequest2InterruptOutThread extends Thread {

        private BaseRequest mBaseRequest;

        writeRequest2InterruptOutThread(BaseRequest baseRequest) {
            this.mBaseRequest = baseRequest;
            this.setName("writeRequest2InterruptOutThread");
        }

        @Override
        public void run() {
            super.run();
            writeRequest2InterruptOutEndpoint(mBaseRequest);
            mSendNextRequestLock.lock();
            mSendNextRequestCondition.signal();
            mSendNextRequestLock.unlock();
        }

        /**
         * Call this method to write data to the interrupt out endpoint of the USB.
         *
         * @param sendRequest request object to send to usb.
         * @see UsbError#CODE_USB_CONNECTION_NOT_ESTABLISHED
         * @see UsbError#CODE_USB_SEND_DATA_FAILED
         */
        void writeRequest2InterruptOutEndpoint(BaseRequest sendRequest) {
            if (mUsbDeviceConnection == null) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "write interrupt out failed, connection has not been established"));
                return;
            }

            mWriteData2InterruptOutEndpointLock.lock();
            try {
                byte[] writeData = sendRequest.getSendData();
                BaseRequestCallback requestCallback = sendRequest.getRequestCallback();

                ByteBuffer writeBuffer = ByteBuffer.wrap(writeData);
                writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
                UsbRequest writeDataRequest = new UsbRequest();
                writeDataRequest.initialize(mUsbDeviceConnection, mUsbEndpointInterruptOut);

                boolean writeData2QueueRet;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    writeData2QueueRet = writeDataRequest.queue(writeBuffer);
                } else {
                    writeData2QueueRet = writeDataRequest.queue(writeBuffer, writeData.length);
                }

                // true if the queueing operation succeeded, false if the queueing operation failed.
                if (!writeData2QueueRet) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "write request data to queue failed");
                    if (requestCallback != null) requestCallback.onSendFailed(UsbError.CODE_WRITE_REQUEST_TO_QUEUE_FAILED);
                    return;
                }

                UsbRequest writeUsbRequest = mUsbDeviceConnection.requestWait();
                if (!writeUsbRequest.equals(writeDataRequest)) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "write request data to usb failed");
                    if (requestCallback != null) requestCallback.onSendFailed(UsbError.CODE_WRITE_REQUEST_WAIT_FAILED);
                    return;
                }

                // Save the write request currently sent.
                mSendingRequest = sendRequest;
                String writeDataHexStr = ByteUtil.convertHexString(writeData);
                String logInfoType = sendRequest.getClass().getSimpleName();
                Log.i(TAG, UsbLogInfo.msg(logInfoType, "send request hex string: " + writeDataHexStr));
                if (requestCallback != null) requestCallback.onSendSuccess();

                // Define a new usb request to receive data
                UsbRequest readDataRequest = new UsbRequest();
                readDataRequest.initialize(mUsbDeviceConnection, mUsbEndpointInterruptIn);
                ByteBuffer readBuff = ByteBuffer.allocate(RECEIVE_BUFF_SIZE_ON_INTERRUPT_OUT);

                boolean readData2QueueRet;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    readData2QueueRet = readDataRequest.queue(readBuff);
                } else {
                    readData2QueueRet = readDataRequest.queue(readBuff, RECEIVE_BUFF_SIZE_ON_INTERRUPT_OUT);
                }

                // true if the queueing operation succeeded, false if the queueing operation failed.
                if (!readData2QueueRet) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "read request data to queue failed");
                    if (requestCallback != null) requestCallback.onReceiveTimeout();
                    return;
                }

                UsbRequest readUsbRequest = mUsbDeviceConnection.requestWait();
                if (!readUsbRequest.equals(readDataRequest)) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "read request data to usb failed");
                    if (requestCallback != null) requestCallback.onReceiveTimeout();
                    return;
                }

                byte[] receiveData = readBuff.array();
                int real_message_length = receiveData[1] & 0x0FF;
                byte[] realData = new byte[real_message_length + BaseRequest.LENGTH_WRITE_REQUEST_HEAD];
                System.arraycopy(receiveData, 0, realData, 0, realData.length);
                Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS,
                        "receive data (interrupt in, len = " + receiveData.length + "): " + ByteUtil.convertHexString(realData)));
                parseReceiveData(receiveData);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mWriteData2InterruptOutEndpointLock.unlock();
            }

        }

    }


    /**
     * This thread is used to send request message.
     * <p>Note: An attribute protocol request and response or indication-confirmation pair is
     * considered a single transaction. A transaction shall always be performed on
     * one ATT Bearer, and shall not be split over multiple ATT Bearers</p>
     */
    private class SendRequestThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                mSendNextRequestLock.lock();
                try {
                    // construct request message.
                    BaseRequest sendRequest = mSendRequestCacheQueue.take();
                    sendRequest.setRequestOpcode();
                    sendRequest.setMessageLength();
                    sendRequest.createRequest();
                    String logInfoType = sendRequest.getClass().getSimpleName();
                    BaseRequestCallback requestCallback = sendRequest.getRequestCallback();

                    // send request message on interrupt out.
                    writeRequest2InterruptOutThread writeRequestThread = new writeRequest2InterruptOutThread(sendRequest);
                    writeRequestThread.start();

                    // Check if the response times out
                    boolean noTimeout = mSendNextRequestCondition.await(MAXIMUM_RESPONSE_TIME_WHEN_SEND_REQUEST, TimeUnit.SECONDS);
                    if (!noTimeout) { // No server response received, write request timeout.
                        Log.e(TAG, UsbLogInfo.msg(logInfoType, "receive server response timeout"));
                        if (requestCallback != null) requestCallback.onReceiveTimeout();
                        clearRequestCacheQueue();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "interrupt send request thread."));
                    break;
                } finally {
                    mSendNextRequestLock.unlock();
                }
            }
        }
    }


    /**
     * Get the log info type sent according to the attribute opcode passed in.
     *
     * @param attributeOpcode attribute opcode of sent att pdu.
     * @return log type string defined in {@link UsbLogInfo}
     */
    private static String getLogInfoTypeByOpcode(int attributeOpcode) {
        String logInfoType = UsbLogInfo.TYPE_UNKNOWN_INFO_TYPE;
        switch (attributeOpcode) {
            case AttPduOpcodeDefine.WRITE_REQUEST:
                logInfoType = UsbLogInfo.TYPE_SEND_WRITE_REQUEST;
                break;
            case AttPduOpcodeDefine.READ_REQUEST:
                logInfoType = UsbLogInfo.TYPE_SEND_READ_REQUEST;
                break;
            case AttPduOpcodeDefine.EXCHANGE_MTU_REQUEST:
                logInfoType = UsbLogInfo.TYPE_EXCHANGE_MTU_REQUEST;
                break;
        }
        return logInfoType;
    }


    /**
     * Use this thread to send write attribute command.
     */
    private class WriteAttributesCommandRunnable implements Runnable {

        private WriteAttributeCommand mWriteAttributesCommand;

        WriteAttributesCommandRunnable(WriteAttributeCommand writeCommand) {
            this.mWriteAttributesCommand = writeCommand;
        }

        @Override
        public void run() {
            mWriteAttributesCommand.createCommand();
            writeCommand2InterruptOutEndpoint(mWriteAttributesCommand);
        }

        void writeCommand2InterruptOutEndpoint(WriteAttributeCommand command) {
            if (mUsbDeviceConnection == null) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "write command to interrupt out failed, connection has not been established"));
                return;
            }

            mWriteData2InterruptOutEndpointLock.lock();
            try {
                byte[] writeData = command.getSendData();
                WriteAttributeCommandCallback writeCommandCallback = command.getWriteAttributeCommandCallback();

                ByteBuffer writeBuffer = ByteBuffer.wrap(writeData);
                writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
                UsbRequest writeDataRequest = new UsbRequest();
                writeDataRequest.initialize(mUsbDeviceConnection, mUsbEndpointInterruptOut);

                boolean writeData2QueueRet;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    writeData2QueueRet = writeDataRequest.queue(writeBuffer);
                } else {
                    writeData2QueueRet = writeDataRequest.queue(writeBuffer, writeData.length);
                }

                // true if the queueing operation succeeded, false if the queueing operation failed.
                if (!writeData2QueueRet) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "write command data to queue failed");
                    if (writeCommandCallback != null) writeCommandCallback.onSendFailed(UsbError.CODE_WRITE_COMMAND_TO_QUEUE_FAILED);
                    return;
                }

                UsbRequest writeUsbRequest = mUsbDeviceConnection.requestWait();
                if (!writeUsbRequest.equals(writeDataRequest)) {
                    Log.e(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "write command data to usb failed");
                    if (writeCommandCallback != null) writeCommandCallback.onSendFailed(UsbError.CODE_WRITE_COMMAND_WAIT_FAILED);
                    return;
                }

                String writeDataHexStr = ByteUtil.convertHexString(writeData);
                String logInfoType = command.getClass().getSimpleName();
                Log.i(TAG, UsbLogInfo.msg(logInfoType, "send command hex string: " + writeDataHexStr));
                if (writeCommandCallback != null) writeCommandCallback.onSendSuccess();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mWriteData2InterruptOutEndpointLock.unlock();
            }
        }

    }


    /**
     * Clear local requests that have not been sent.
     * <p> This method will be called when a request is sent and the corresponding response is not received within 30s </p>
     */
    private void clearRequestCacheQueue() {
        if (mSendRequestCacheQueue != null) mSendRequestCacheQueue.clear();
    }

    /**
     * Call this method to start listening for data from the USB endpoint.
     * <p>Note: This operation needs to wait for the USB connection to be established.</p>
     *
     * @return connect result, The connection may fail because some operations may not be ready. If the connection fails,
     * Some error codes will be returned.
     * @see UsbError
     */
    public int connect() {
        // check usb connection
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_CONNECT, "connect failed, usb connection has not been established"));
            return UsbError.CODE_USB_CONNECTION_NOT_ESTABLISHED;
        }

        // check bulk out endpoint 
        // TODO: 2020/1/2  Comment the bulk out check, because the bulk out driver of the hardware is not ready yet.
        /*if (mUsbEndpointBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_CONNECT, "connect failed, can not found usb bulk out endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }*/

        // check bulk in endpoint, This endpoint may not exist sometimes
        if (mUsbEndpointBulkIn != null) {
            startListenBulkInData();
        }

        // check interrupt in endpoint,
        if (mUsbEndpointInterruptIn != null) {
            // startListenInterruptInData();
        }

        // check interrupt out endpoint, send Att pdu and usb command data on this endpoint.
        if (mUsbEndpointInterruptOut != null) { // start the thread to receive data from the user
            startReceivingRequestData();
            startReceivingWriteCommandData();
            Log.d(TAG, UsbLogInfo.TYPE_RUNNING_TIPS + "The receive buffer size of interrupt out is " + RECEIVE_BUFF_SIZE_ON_INTERRUPT_OUT);
        }

        // check bulk out endpoint, send audio data on this endpoint.
        if (mUsbEndpointBulkOut != null) {

        }

        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Call this method will disconnect all connections related to USB. If you call this method, Until you call
     * the {@link LocalUsbConnector#connect()} method again to establish a connection, otherwise you will not be able
     * to do the following:
     * <ul>
     * <li>send Request: {@link LocalUsbConnector#sendRequest(BaseRequest)}</li>
     * <li>Write Command: {@link LocalUsbConnector#writeAttributesCommand(WriteAttributeCommand)}</li>
     * </ul>
     * <p>In addition, write requests and write commands that have not been sent in the cache queue will be discarded</p>
     *
     * @see LocalUsbConnector#connect()
     * @see LocalUsbConnector#sendRequest(BaseRequest)
     * @see LocalUsbConnector#writeAttributesCommand(WriteAttributeCommand)
     */
    public void disConnect() {
        // 1. stop receiving data incoming.
        stopReceivingRequestData();
        stopReceivingWriteCommandData();
        // 2. stop listening thread
        stopListenBulkInData();
        stopListenInterruptInData();
        // 3. clear resource (selectDevice, interface, endpoint, usb connection, etc).
        mSelectUsbDevice = null;
        // 4. destroy usb connection (Test)
        /*if (mUsbDeviceConnection != null) {
            mUsbDeviceConnection.close();
            mUsbDeviceConnection = null;
        }*/
    }

}
