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

import com.realsil.sdk.core.usb.connector.att.WriteAttributeRequestCallback;
import com.realsil.sdk.core.usb.connector.att.AttributeOpcode;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.OnServerTransactionChangeCallback;
import com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest;
import com.realsil.sdk.core.usb.connector.util.ByteUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
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
    private static final int BULK_TRANSFER_RECEIVE_MAX_TIMEOUT = 10000;

    private Lock mGrantAccessUsbLock;
    private Lock mSendNextWriteRequestLock;
    private Lock mWriteData2BulkOutLock;

    private Condition mGrantAccessUsbCondition;
    private Condition mSendNextWriteRequestCondition;

    private CopyOnWriteArrayList<OnServerTransactionChangeCallback> mServerTransactionChangeCallbacks;

    /**
     * Maximum response time when send a write request to server, in second.
     * <p>A transaction not completed within 30 seconds shall time out. Such a transaction
     * shall be considered to have failed and the local higher layers shall be informed of this
     * failure. No more attribute protocol requests, commands, indications or notifications
     * shall be sent to the target device on this ATT Bearer </p>
     */
    private static final int MAXIMUM_RESPONSE_TIME_WHEN_SEND_WRITE_REQUEST = 30;

    /**
     * A listening flag is used to determine whether the listening thread is working.
     *
     * @see LocalUsbConnector#startListenBulkInData()
     * @see LocalUsbConnector#startListenInterruptInData()
     */
    private AtomicBoolean mListeningFlag;

    /**
     * Record the Write Request currently being sent. Only one write request can be sent
     * at a time. You must wait for the corresponding write response before sending the next write request.
     */
    private WriteAttributeRequest mSendingWriteAttributesRequest;

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
     * @see LocalUsbConnector#startReceivingWriteRequestData()
     */
    private Thread mSendWriteAttributesRequestThread;

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
                if (instance == null) {
                    instance = new LocalUsbConnector();
                }
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
            initObjectLock();
            mListeningFlag = new AtomicBoolean(false);
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "context parameter can not be null"));
            return UsbError.CODE_CONTEXT_IS_NULL;
        }
        return UsbError.CODE_NO_ERROR;
    }


    private void initObjectLock() {
        mGrantAccessUsbLock = new ReentrantLock();
        mSendNextWriteRequestLock = new ReentrantLock();
        mGrantAccessUsbCondition = mGrantAccessUsbLock.newCondition();
        mSendNextWriteRequestCondition = mSendNextWriteRequestLock.newCondition();
        mWriteData2BulkOutLock = new ReentrantLock();
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
            mGrantAccessUsbLock.lock();
            try {
                PendingIntent requestUsbIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(UsbAction.ACTION_REQUEST_USB_PERMISSION), 0);
                mUsbManager.requestPermission(mSelectUsbDevice, requestUsbIntent);
                mGrantAccessUsbCondition.await(5000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mGrantAccessUsbLock.unlock();
            }
        }

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
            return UsbError.CODE_DEVICE_IS_NOT_AUTHORIZED;
        }

        return UsbError.CODE_NO_ERROR;
    }

    /**
     * Configure the selected device. The configured device must be authorized.If the device is not authorized,
     * please call {@link LocalUsbConnector#authorizeDevice()}method to authorize.
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
                if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
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

        // check bulk in interface
        if (mUsbInterfaceBulkIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk in interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }

        // check bulk out interface
        if (mUsbInterfaceBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk out interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }

        // check interrupt in interface
        if (mUsbInterfaceInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb interrupt in interface"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_INTERFACE;
        }

        // Check bulk in endpoint
        if (mUsbEndpointBulkIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        // check bulk out endpoint
        if (mUsbEndpointBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb bulk out endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        // check interrupt in endpoint
        if (mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not found usb interrupt in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "The required endpoint has been found"));

        // Open Usb Connection
        mUsbDeviceConnection = mUsbManager.openDevice(mSelectUsbDevice);
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, can not open the usb connection"));
            return UsbError.CODE_OPEN_USB_CONNECTION_FAILED;
        }

        // claim interrupt in interface
        boolean holdInterruptInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceInterruptIn, true);
        if (!holdInterruptInRet) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim interrupt in interface failed"));
            return UsbError.CODE_HOLD_USB_INTERFACE;
        }

        // claim bulk in interface
        boolean holdBulkInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkIn, true);
        if (!holdBulkInRet) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim bulk in interface failed"));
            return UsbError.CODE_HOLD_USB_INTERFACE;
        }

        // claim bulk out interface
        boolean holdBulkOutRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkOut, true);
        if (!holdBulkOutRet) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_INIT_USB_CONNECTOR, "setup failed, claim bulk out interface failed"));
            return UsbError.CODE_HOLD_USB_INTERFACE;
        }

        return UsbError.CODE_NO_ERROR;
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
                mGrantAccessUsbLock.lock();
                try {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    if (device != null && granted) {
                        mSelectUsbDevice = device;
                    } else {
                        mSelectUsbDevice = null;
                    }
                    mGrantAccessUsbCondition.signal();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mGrantAccessUsbLock.unlock();
                }
            } else if (Objects.equals(intent.getAction(), UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "device has detached, need to re-establish connection"));
            }
        }
    };


    /**
     * Call this method to add a callback for listening to the indication and notification messages returned by the server.
     *
     * @param callback A callback interface for listening notification and indication message from the server.
     * @see OnServerTransactionChangeCallback
     */
    public void addOnServerTransactionChangeCallback(OnServerTransactionChangeCallback callback) {
        if (callback == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "callback parameter can not be null"));
            return;
        }

        if (mServerTransactionChangeCallbacks == null) {
            mServerTransactionChangeCallbacks = new CopyOnWriteArrayList<>();
            mServerTransactionChangeCallbacks.add(callback);
        } else {
            if (!mServerTransactionChangeCallbacks.contains(callback)) {
                mServerTransactionChangeCallbacks.add(callback);
            }
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
     * Listen for data from the usb bulk in endpoint
     */
    private class ListenUsbBulkInDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (mListeningFlag.get()) {
                byte[] recvBuf = new byte[mUsbEndpointBulkIn.getMaxPacketSize()];
                int recvlen = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkIn, recvBuf, recvBuf.length, BULK_TRANSFER_RECEIVE_MAX_TIMEOUT);
                if (recvlen > 0) {
                    // Parse receive data
                    byte[] recvData = new byte[recvlen];
                    System.arraycopy(recvBuf, 0, recvData, 0, recvlen);
                    parseReceiveData(recvData);
                } else {
                    Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "receive data failed"));
                }
            }
        }
    }

    /**
     * Listen for data from the usb interrupt in endpoint
     */
    private class ListenUsbInterruptInDataThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (mListeningFlag.get()) {
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
                    parseReceiveData(recvData);
                    Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "interrupt in endpoint has receive data, length: " + recvData.length));
                }
            }
        }
    }


    /**
     * According to the opcode returned by the server, call the corresponding parsing method to parse the data returned by the server.
     *
     * @param receiveData Data returned by the server.
     */
    private void parseReceiveData(byte[] receiveData) {
        byte receive_att_opcode = receiveData[0];
        switch (receive_att_opcode) {
            case AttributeOpcode.WRITE_RESPONSE:
                parseWriteResponseData(receiveData);
                break;
            case AttributeOpcode.READ_RESPONSE:

                break;
            case AttributeOpcode.HANDLE_VALUE_INDICATION:
                parseIndicationMessageFromServer(receiveData);
                break;
            case AttributeOpcode.HANDLE_VALUE_NOTIFICATION:
                parseNotificationMessageFromServer(receiveData);
                break;
            case AttributeOpcode.ERROR_RESPONSE:
                parseErrorResponseData(receiveData);
                break;
            default:
                break;
        }
    }


    /**
     * Call this method to parse the received write response data.
     *
     * @param writeResponse Write response data returned by the server
     * @see LocalUsbConnector#writeAttributesRequest(WriteAttributeRequest)
     */
    private void parseWriteResponseData(byte[] writeResponse) {
        mSendNextWriteRequestLock.lock();
        try {
            if (mSendingWriteAttributesRequest != null) {
                mSendingWriteAttributesRequest.parseResponse(writeResponse);
                int parseResult = mSendingWriteAttributesRequest.getParseResult();
                if (parseResult == AttributeParseResult.PARSE_SUCCESS) {
                    mSendNextWriteRequestCondition.signal();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mSendNextWriteRequestLock.unlock();
        }
    }

    /**
     * Call this method to parse the received write response data.
     * @param readResponse Read response data returned by the server.
     * @see LocalUsbConnector#readAttributesRequest(ReadAttributeRequest)
     */
    private void parseReadResponseData(byte[] readResponse) {

    }


    private void parseIndicationMessageFromServer(byte[] indicationData) {
        if (mServerTransactionChangeCallbacks != null) {
            for (OnServerTransactionChangeCallback callback : mServerTransactionChangeCallbacks) {
                callback.onReceiveIndicationMessage(indicationData);
            }
        }
    }


    private void parseNotificationMessageFromServer(byte[] notificationData) {
        if (mServerTransactionChangeCallbacks != null) {
            for (OnServerTransactionChangeCallback callback : mServerTransactionChangeCallbacks) {
                callback.onReceiveNotificationMessage(notificationData);
            }
        }
    }


    private void parseErrorResponseData(byte[] errorResponseData) {
        byte requestOpcodeInError = errorResponseData[1];
        if (requestOpcodeInError == AttributeOpcode.WRITE_REQUEST) {
            parseWriteResponseData(errorResponseData);
        }
    }


    /**
     * Calling this method will create a cache queue to store the write request that the user
     * will send, and start a thread to send the write attribute request in cache queue.
     */
    private void startReceivingWriteRequestData() {
        if (mSendWriteRequestCacheQueue == null) {
            // Create a new cache queue for storing write attribute request.
            mSendWriteRequestCacheQueue = new LinkedBlockingQueue<>();
        }

        if (mSendWriteAttributesRequestThread == null) {
            mSendWriteAttributesRequestThread = new SendWriteAttributesRequestThread();
            mSendWriteAttributesRequestThread.start();
        }
    }

    private void stopReceivingWriteRequestData() {
        if (mSendWriteAttributesRequestThread != null) {
            mSendWriteAttributesRequestThread.interrupt();
            mSendWriteAttributesRequestThread = null;
        }

        if (mSendWriteRequestCacheQueue != null) {
            mSendWriteRequestCacheQueue.clear();
            mSendWriteRequestCacheQueue = null;
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
     * Call this method to write a attribute value to the server.
     * <p>You can add a callback method {@link WriteAttributeRequest#addWriteAttributeRequestCallback(WriteAttributeRequestCallback)} on
     * the {@link WriteAttributeRequest} object to monitor the execution status of this  instruction</p>
     *
     * @param writeAttributesRequest An entity object encapsulates some information of the attribute when writing the request.
     * @see WriteAttributeRequest#addWriteAttributeRequestCallback(WriteAttributeRequestCallback)
     */
    public void writeAttributesRequest(WriteAttributeRequest writeAttributesRequest) {
        if (writeAttributesRequest == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "send failed, argus can not be null"));
            return;
        }

        if (mSendWriteRequestCacheQueue != null) {
            mSendWriteRequestCacheQueue.offer(writeAttributesRequest);
        } else {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "send failed, connection has not been established"));
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

    public void readAttributesRequest(ReadAttributeRequest readAttributesRequest) {

    }

    /**
     * A blocked buffer queue for storing write request messages.
     * <p>When the usb connection is disconnected, this queue needs to be cleared.</p>
     */
    private LinkedBlockingQueue<WriteAttributeRequest> mSendWriteRequestCacheQueue;

    /**
     * Call this method to write data to the bulk out endpoint of the USB.
     *
     * @param writeData Data to be written.
     * @return Written result. length of data transferred (or zero) for success, or negative value for failure
     * @see UsbDeviceConnection#bulkTransfer(UsbEndpoint, byte[], int, int, int)
     * @see UsbError#CODE_USB_CONNECTION_NOT_ESTABLISHED
     * @see UsbError#CODE_USB_SEND_DATA_FAILED
     */
    private int writeData2BulkOutEndpoint(byte[] writeData) {
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_RUNNING_TIPS, "write bulk failed, connection has not been established"));
            return UsbError.CODE_USB_CONNECTION_NOT_ESTABLISHED;
        }
        mWriteData2BulkOutLock.lock();
        int writeRet = -1;
        try {
            writeRet = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkOut, writeData, writeData.length, BULK_TRANSFER_SEND_MAX_TIMEOUT);
            if (writeRet < 0) return UsbError.CODE_USB_SEND_DATA_FAILED;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWriteData2BulkOutLock.unlock();
        }
        return writeRet;
    }


    /**
     * This thread is used to send write attribute request.
     * <p>An attribute protocol request and response or indication-confirmation pair is
     * considered a single transaction. A transaction shall always be performed on
     * one ATT Bearer, and shall not be split over multiple ATT Bearers</p>
     */
    private class SendWriteAttributesRequestThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                mSendNextWriteRequestLock.lock();
                try {
                    WriteAttributeRequest writeAttributesRequest = mSendWriteRequestCacheQueue.take();
                    writeAttributesRequest.createRequest();
                    byte[] sendData = writeAttributesRequest.getSendData();
                    String sendHexStr = ByteUtil.printHexString(sendData);
                    Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "send write request hex string: " + sendHexStr));

                    int writeRet = writeData2BulkOutEndpoint(sendData);

                    WriteAttributeRequestCallback callback = mSendingWriteAttributesRequest.getWriteAttributeRequestCallback();
                    if (writeRet >= 0) {
                        Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "write bulk success, result is " + writeRet));
                        // Record the write request currently sent.
                        mSendingWriteAttributesRequest = writeAttributesRequest;
                        if (callback != null) callback.onRequestSendSuccess();

                        // If the thread has not been woken up within 30 seconds, the previous task is considered to have failed to send.
                        boolean noTimeout = mSendNextWriteRequestCondition.await(MAXIMUM_RESPONSE_TIME_WHEN_SEND_WRITE_REQUEST, TimeUnit.SECONDS);
                        if (!noTimeout) { // No write response received, write request timeout.
                            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "receive write response timeout"));
                            disConnect();
                            if (callback != null) callback.onWriteTimeout();
                        } else {
                            Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "has received a write response from server"));
                        }
                    } else {
                        Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "send write request failed, write bulk failed, error: " + writeRet));
                        callback.onRequestSendFailed(writeRet);
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_REQUEST, "interrupt send write request thread."));
                    break;
                } finally {
                    mSendNextWriteRequestLock.unlock();
                }
            }
        }
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
            byte[] sendData = mWriteAttributesCommand.getSendData();
            String sendHexStr = ByteUtil.printHexString(sendData);
            Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_COMMAND, "send write command hex string: " + sendHexStr));
            int writeRet = writeData2BulkOutEndpoint(sendData);
            if (writeRet < 0) {
                Log.i(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_SEND_WRITE_COMMAND, "send write command failed, write bulk failed, error: " + writeRet));
            }
        }
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

        // check bulk in endpoint
        if (mUsbEndpointBulkIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_CONNECT, "connect failed, can not found usb bulk in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        // check bulk out endpoint
        if (mUsbEndpointBulkOut == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_CONNECT, "connect failed, can not found usb bulk out endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        // check interrupt in endpoint
        if (mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_CONNECT, "connect failed, can not found usb interrupt in endpoint"));
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        // start the thread to receive data from the user
        startReceivingWriteRequestData();
        startReceivingWriteCommandData();

        // start thread to read the data of the bulk in and interrupt in endpoints.
        mListeningFlag.set(true);
        startListenBulkInData();
        startListenInterruptInData();

        return UsbError.CODE_NO_ERROR;
    }


    /**
     * Call this method will disconnect all connections related to USB. If you call this method, Until you call
     * the {@link LocalUsbConnector#connect()} method again to establish a connection, otherwise you will not be able
     * to do the following:
     * <ul>
     * <li>Write Request: {@link LocalUsbConnector#writeAttributesRequest(WriteAttributeRequest)}</li>
     * <li>Write Command: {@link LocalUsbConnector#writeAttributesCommand(WriteAttributeCommand)}</li>
     * </ul>
     * <p>In addition, write requests and write commands that have not been sent in the cache queue will be discarded</p>
     *
     * @see LocalUsbConnector#connect()
     * @see LocalUsbConnector#writeAttributesRequest(WriteAttributeRequest)
     * @see LocalUsbConnector#writeAttributesCommand(WriteAttributeCommand)
     */
    public void disConnect() {
        // Stop all working threads(listening bulk in thread, listening interrupt in thread, take write attribute thread)
        /*if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbError.STR_USB_CONNECTION_NOT_ESTABLISHED);
            return;
        }

        if (mUsbEndpointBulkIn == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT + ", bulk in endpoint can not found.");
            return;
        }

        if (mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT + ", interrupt in endpoint can not found");
            return;
        }

        if (!mUsbDeviceConnection.releaseInterface(mUsbInterfaceBulkIn)) {
            Log.e(TAG, UsbError.STR_USB_RELEASE_INTERFACE_FAILED + ", release bulk in failed");
            return;
        }

        if (!mUsbDeviceConnection.releaseInterface(mUsbInterfaceInterruptIn)) {
            Log.e(TAG, UsbError.STR_USB_RELEASE_INTERFACE_FAILED + ", release interrupt in failed");
            return;
        }

        if (!mUsbDeviceConnection.releaseInterface(mUsbInterfaceBulkOut)) {
            Log.e(TAG, UsbError.STR_USB_RELEASE_INTERFACE_FAILED + ", release bulk out failed");
            return;
        }*/

        if (mSendWriteAttributesRequestThread == null || mSendWriteRequestCacheQueue == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_DISCONNECT, "disconnect failed, connection has not been established (0)"));
            return;
        }

        if (mSendWriteCommandExecutor == null) {
            Log.e(TAG, UsbLogInfo.msg(UsbLogInfo.TYPE_CALL_DISCONNECT, "disconnect failed, connection has not been established (1)"));
            return;
        }

        // 1. stop receiving write request data incoming.
        stopReceivingWriteRequestData();
        // 2. stop receiving write command data incoming.
        stopReceivingWriteCommandData();
        // TODO: 2019/12/23 stop receiving read request data incoming.
    }


}
