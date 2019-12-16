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

import com.realsil.sdk.core.usb.connector.att.AttributeCommCallback;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributesCommand;
import com.realsil.sdk.core.usb.connector.att.impl.WriteAttributesRequest;
import com.realsil.sdk.core.usb.connector.util.ByteUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private UsbDevice  mSelectUsbDevice;

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

    private UsbDeviceConnection mUsbDeviceConnection;

    private Context mContext = null;

    private static final int BULK_TRANSFER_SEND_MAX_TIMEOUT    = 5000;
    private static final int BULK_TRANSFER_RECEIVE_MAX_TIMEOUT = 10000;

    private Lock mGrantAccessUsbLock;
    private Lock mSendNextWriteRequestLock;
    private Lock mWriteData2BulkOutLock;

    private Condition mGrantAccessUsbCondition;
    private Condition mSendNextWriteRequestCondition;

    private boolean       mAllowOnlyHaveOutEndpoint;
    private AtomicBoolean mRunningFlag;


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


    public int initConnector(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
            mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            if (mUsbManager == null) {
                Log.e(TAG, UsbError.STR_CONTEXT_GET_USB_MANAGER_FAILED);
                return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
            }

            // Add receive to listen connection process.
            initReceiver();
            initObjectLock();
            initThreadPool();

        } else {
            throw new IllegalArgumentException(UsbError.STR_CONTEXT_IS_NULL);
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

    public int searchUsbDevice(int vendorId, int productId) {
        if (mUsbManager == null) {
            Log.e(TAG, UsbError.STR_CONTEXT_GET_USB_MANAGER_FAILED);
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_DEVICE);
            return UsbError.CODE_CAN_NOT_FOUND_USB_DEVICE;
        }

        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice usbDevice = iterator.next();
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
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE + ", vendorId: " + vendorId + ", productId: " + productId);
            return UsbError.CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
        }
        Log.i(TAG, UsbLogInfo.INFO_GET_USB_DEVICE_SUCCESSFULLY);
        return UsbError.CODE_NO_ERROR;
    }


    public int searchUsbDevice() {
        return searchUsbDevice(0, 0);
    }


    public int authorizeDevice() {
        if (mUsbManager == null) {
            Log.e(TAG, UsbError.STR_CONTEXT_GET_USB_MANAGER_FAILED);
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        if (mSelectUsbDevice == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE);
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
                Log.i(TAG, UsbLogInfo.INFO_DEVICE_IS_AUTHORIZED
                        + "\nDevice Name: " + mSelectUsbDevice.getDeviceName()
                        + "\nDevice Product Name: " + mSelectUsbDevice.getProductName()
                        + "\nDevice Serial Number: " + mSelectUsbDevice.getSerialNumber());
            } else {
                Log.i(TAG, UsbLogInfo.INFO_DEVICE_IS_AUTHORIZED + "\nDevice Name: " + mSelectUsbDevice.getDeviceName());
            }
        } else {
            Log.e(TAG, UsbError.STR_DEVICE_IS_NOT_AUTHORIZED);
            return UsbError.CODE_DEVICE_IS_NOT_AUTHORIZED;
        }

        return UsbError.CODE_NO_ERROR;
    }


    public int setupDevice() {
        if (mUsbManager == null) {
            Log.e(TAG, UsbError.STR_CONTEXT_GET_USB_MANAGER_FAILED);
            return UsbError.CODE_CONTEXT_GET_USB_MANAGER_FAILED;
        }

        if (mSelectUsbDevice == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE);
            return UsbError.CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
        }

        if (!mUsbManager.hasPermission(mSelectUsbDevice)) {
            Log.e(TAG, UsbError.STR_DEVICE_IS_NOT_AUTHORIZED + " , Please call LocalUsbConnector#requestAuthorizationDevice() to authorize current device");
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
                // Bulk transmission
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

                // Interrupt transmission
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

        // Check UsbEndpoint In
        if (!mAllowOnlyHaveOutEndpoint) {
            if (mUsbEndpointBulkIn == null) {
                Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT);
                return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
            }
        }

        // Check UsbEndpoint Out
        if (mUsbEndpointBulkOut == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT);
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        Log.i(TAG, UsbLogInfo.INFO_FOUND_THE_SPECCIFIED_USB_ENDPOINT);

        /*if (mUsbEndpointBulkIn == null || mUsbEndpointBulkOut == null || mUsbEndpointControlIn == null || mUsbEndpointControlOut == null) {
            mUsbEndpointBulkIn = null;
            mUsbEndpointBulkOut = null;
            mUsbEndpointControlIn = null;
            mUsbEndpointControlOut= null;
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT);
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }*/

        // Open Usb Connection
        mUsbDeviceConnection = mUsbManager.openDevice(mSelectUsbDevice);
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbError.STR_OPEN_USB_CONNECTION_FAILED);
            return UsbError.CODE_OPEN_USB_CONNECTION_FAILED;
        }

        if (!mAllowOnlyHaveOutEndpoint) {
            boolean holdBulkInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkIn, true);
            if (!holdBulkInRet) {
                Log.e(TAG, UsbError.STR_HOLD_USB_INTERFACE + ", bulk in error");
                return UsbError.CODE_HOLD_USB_INTERFACE;
            }
        }

        boolean holdInterruptInRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceInterruptIn, true);
        if (!holdInterruptInRet) {
            Log.e(TAG, UsbError.STR_HOLD_USB_INTERFACE + ", Interrupt in error");
            return UsbError.CODE_HOLD_USB_INTERFACE;
        }

        boolean holdBulkOutRet = mUsbDeviceConnection.claimInterface(mUsbInterfaceBulkOut, true);
        if (!holdBulkOutRet) {
            Log.e(TAG, UsbError.STR_HOLD_USB_INTERFACE + ", bulk out error");
            return UsbError.CODE_HOLD_USB_INTERFACE;
        }

        return UsbError.CODE_NO_ERROR;
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbAction.ACTION_REQUEST_USB_PERMISSION);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private void destroyReceiver() {
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
            }
        }
    };


    public int startReadingFromFromUsb() {


        if (!mStartWorkFlag) {
            mStartWorkFlag = true;
            new ReadUsbBulkInDataWorker().start();
        }

    }

    private void startReadingFromFromUsb2() {
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbError.STR_USB_CONNECTION_NOT_ESTABLISHED);
            return;
        }

        int recvMaxSize = mUsbEndpointBulkIn.getMaxPacketSize();
        ByteBuffer buffer = ByteBuffer.allocate(recvMaxSize);
        UsbRequest usbRequest = new UsbRequest();
        usbRequest.initialize(mUsbDeviceConnection, mUsbEndpointInterruptIn);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            usbRequest.queue(buffer);
        } else {
            usbRequest.queue(buffer, recvMaxSize);
        }
        if (mUsbDeviceConnection.requestWait() == usbRequest) {
            byte[] retData = buffer.array();
            Log.i(TAG, "has received data, data length: " + retData.length);
        }
    }


    private ListenUsbBulkInDataThread      mListenUsbBulkInDataThread;
    private ListenUsbInterruptInDataThread mListenUsbInterruptInDataThread;

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
            while (mRunningFlag.get()) {
                byte[] recvBuf = new byte[mUsbEndpointBulkIn.getMaxPacketSize()];
                int recvlen = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkIn, recvBuf, recvBuf.length, BULK_TRANSFER_RECEIVE_MAX_TIMEOUT);
                if (recvlen > 0) {
                    // Parse receive data
                    byte[] recvData = new byte[recvlen];
                    System.arraycopy(recvBuf, 0, recvData, 0, recvlen);
                    ParseWriteResponseRunnable parseRunnable = new ParseWriteResponseRunnable(recvData);
                    mParseWriteResponseThreadPool.execute(parseRunnable);
                } else {
                    Log.e(TAG, UsbError.STR_USB_RECEIVE_DATA_FAILED);
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
            while (mRunningFlag.get()) {
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
                    ParseWriteResponseRunnable parseRunnable = new ParseWriteResponseRunnable(recvData);
                    mParseWriteResponseThreadPool.execute(parseRunnable);
                    Log.i(TAG, "has received data, data length: " + recvData.length);
                }
            }
        }
    }


    /**
     * Call this method to start listening for data from the USB endpoint.
     * <p>Note: This operation needs to wait for the USB connection to be established.</p>
     * @return
     */
    public int connect() {
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, UsbError.STR_USB_CONNECTION_NOT_ESTABLISHED);
            return UsbError.CODE_USB_CONNECTION_NOT_ESTABLISHED;
        }

        if (mUsbEndpointBulkIn == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT + ", bulk in endpoint can not found.");
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        if (mUsbEndpointInterruptIn == null) {
            Log.e(TAG, UsbError.STR_CAN_NOT_FOUND_USB_ENDPOINT + ", interrupt in endpoint can not found");
            return UsbError.CODE_CAN_NOT_FOUND_USB_ENDPOINT;
        }

        disConnect();
        startListenBulkInData();
        startListenInterruptInData();
        return UsbError.CODE_NO_ERROR;
    }

    public void disConnect() {
        // Stop all working threads(listening bulk in thread, listening interrupt in thread, take write attribute thread)
        mRunningFlag.set(false);


    }

    public void start() {
        // stop all working thread before call this method

        // Start usb bulk in endpoint listening....
        // Start usb interrupt in endpoint listening....
        // Take a request from the attRequestPdu cache queue to send

    }

    public void stop() {
        //
        //
    }

    /// Send write attribute command thread pool args
    /**
     * Core thread num of send write command thread pool
     */
    private static final int                CORE_THREAD_NUM_SEND_WRITE_COMMAND = 10;
    /**
     * Max thread num of send write command thread pool
     */
    private static final int                MAX_THREAD_NUM_SEND_WRITE_COMMAND  = 10;
    /**
     * Keep alive time of idle thread in send write command thread pool
     */
    private static final int                KEEP_ALIVE_TIME_SEND_WRITE_COMMAND = 1000;
    /**
     * Thread pool for executing send write command tasks
     */
    private              ThreadPoolExecutor mSendWriteCommandThreadPool;

    /// Parse write response thread pool args
    /**
     * Core thread num of parse write response thread pool
     */
    private static final int                CORE_THREAD_NUM_PARSE_WRITE_RESPONSE = 10;
    /**
     * Max thread num of parse write response thread pool
     */
    private static final int                MAX_THREAD_NUM_PARSE_WRITE_RESPONSE  = 10;
    /**
     * Keep alive time of parse write response thread pool
     */
    private static final int                KEEP_ALIVE_TIME_PARSE_WRITE_RESPONSE = 1000;
    /**
     * Thread pool for executing parse write response tasks
     */
    private              ThreadPoolExecutor mParseWriteResponseThreadPool;

    private void initThreadPool() {
        mRunningFlag = new AtomicBoolean(false);

        // Init threadPool, This thread pool is used to send commands related to Command PDU.
        LinkedBlockingQueue<Runnable> writeAttributeCommandCacheQueue = new LinkedBlockingQueue<Runnable>();
        mSendWriteCommandThreadPool = new ThreadPoolExecutor(
                CORE_THREAD_NUM_SEND_WRITE_COMMAND,
                MAX_THREAD_NUM_SEND_WRITE_COMMAND,
                KEEP_ALIVE_TIME_SEND_WRITE_COMMAND,
                TimeUnit.MILLISECONDS,
                writeAttributeCommandCacheQueue,
                new ThreadPoolExecutor.AbortPolicy()
        );

        LinkedBlockingQueue<Runnable> parseWriteResponseCacheQueue = new LinkedBlockingQueue<Runnable>();
        mParseWriteResponseThreadPool = new ThreadPoolExecutor(
                CORE_THREAD_NUM_PARSE_WRITE_RESPONSE,
                MAX_THREAD_NUM_PARSE_WRITE_RESPONSE,
                KEEP_ALIVE_TIME_PARSE_WRITE_RESPONSE,
                TimeUnit.MILLISECONDS,
                parseWriteResponseCacheQueue,
                new ThreadPoolExecutor.AbortPolicy()
        );

        // Cache write attributes request.
        mSendWriteRequestCacheQueue = new LinkedBlockingQueue<>();
        mSendWriteRequestExecutor = Executors.newSingleThreadExecutor();
    }

    public void setAllowOnlyHaveOutEndpoint(boolean isAllow) {
        this.mAllowOnlyHaveOutEndpoint = isAllow;
    }




    /**
     * Call this method to write a attribute value (typically into a control-point attribute) to the server.
     * <p>Note: No Error Response or Write Response shall be sent in response to this
     * command. If the server cannot write this attribute for any reason the command
     * shall be ignored.</p>
     *
     * @param writeAttributesCommand An entity object that encapsulates some related information of the Attribute
     * @see WriteAttributesCommand
     */
    public void writeAttributesCommand(WriteAttributesCommand writeAttributesCommand) {
        WriteAttributesCommandRunnable runnable = new WriteAttributesCommandRunnable(writeAttributesCommand);
        mSendWriteCommandThreadPool.execute(runnable);
    }

    /**
     * Call this method to write a attribute value to the server.
     * <p>You can add a callback method {@link WriteAttributesRequest#addAttributeCommCallback(AttributeCommCallback)} on
     * the {@link WriteAttributesRequest} object to monitor the execution status of this  instruction</p>
     *
     * @param writeAttributesRequest An entity object encapsulates some information of the attribute when writing the request.
     * @see WriteAttributesRequest#addAttributeCommCallback(AttributeCommCallback)
     */
    public void writeAttributesRequest(WriteAttributesRequest writeAttributesRequest) {
        WriteAttributesRequestRunnable runnable = new WriteAttributesRequestRunnable(writeAttributesRequest);
        mSendWriteRequestCacheQueue.offer(runnable);
    }


    /**
     * A blocked buffer queue for storing write request messages.
     * <p>When the usb connection is disconnected, this queue needs to be cleared.</p>
     */
    private LinkedBlockingQueue<WriteAttributesRequestRunnable> mSendWriteRequestCacheQueue;

    private ExecutorService mSendWriteRequestExecutor;


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
        mWriteData2BulkOutLock.lock();
        int writeRet = -1;
        try {
            if (mUsbDeviceConnection == null) {
                Log.e(TAG, UsbError.STR_USB_CONNECTION_NOT_ESTABLISHED);
                return UsbError.CODE_USB_CONNECTION_NOT_ESTABLISHED;
            }

            writeRet = mUsbDeviceConnection.bulkTransfer(mUsbEndpointBulkOut, writeData, writeData.length, BULK_TRANSFER_SEND_MAX_TIMEOUT);
            if (writeRet < 0) {
                Log.e(TAG, UsbError.STR_USB_SEND_DATA_FAILED);
                return UsbError.CODE_USB_SEND_DATA_FAILED;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWriteData2BulkOutLock.unlock();
        }
        return writeRet;
    }


    private WriteAttributesRequest mSendingWriteAttributesRequest;

    private class SendWriteAttributesRequestThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (mRunningFlag.get()) {
                mSendNextWriteRequestLock.lock();
                try {
                    WriteAttributesRequestRunnable writeRequestRunnable = mSendWriteRequestCacheQueue.take();
                    mSendWriteRequestExecutor.execute(writeRequestRunnable);
                    mSendNextWriteRequestCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
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

        private WriteAttributesCommand mWriteAttributesCommand;

        WriteAttributesCommandRunnable(WriteAttributesCommand writeCommand) {
            this.mWriteAttributesCommand = writeCommand;
        }

        @Override
        public void run() {
            mWriteAttributesCommand.createCommand();
            byte[] sendData = mWriteAttributesCommand.getSendData();
            String sendDataHexStr = ByteUtil.printHexString(sendData);
            Log.i(TAG, "write attribute command: " + sendDataHexStr);
            int writeRet = writeData2BulkOutEndpoint(sendData);
        }
    }


    /**
     * Use this thread to send write attribute request.
     */
    private class WriteAttributesRequestRunnable implements Runnable {

        private WriteAttributesRequest mWriteAttributesRequest;

        WriteAttributesRequestRunnable(WriteAttributesRequest writeRequest) {
            this.mWriteAttributesRequest = writeRequest;
        }

        @Override
        public void run() {
            mWriteAttributesRequest.createRequest();
            byte[] sendData = mWriteAttributesRequest.getSendData();
            String sendDataHexStr = ByteUtil.printHexString(sendData);
            Log.i(TAG, "write attribute request: " + sendDataHexStr);
            int writeRet = writeData2BulkOutEndpoint(sendData);

            // Record the write request currently sent.
            AttributeCommCallback attributeCommCallback = mSendingWriteAttributesRequest.getAttributeCommCallback();
            if (writeRet >= 0) {
                mSendingWriteAttributesRequest = mWriteAttributesRequest;
                if (attributeCommCallback != null) {
                    attributeCommCallback.onSendSuccess();
                }
            } else {
                attributeCommCallback.onSendFailed(writeRet);
            }
        }
    }


    private static final int LENGTH_WRITE_RESPONSE_PDU = 1;

    /**
     * The work of this thread is to parse the content of the write response returned by the server.
     *
     * @see LocalUsbConnector#writeAttributesRequest(WriteAttributesRequest)
     */
    private class ParseWriteResponseRunnable implements Runnable {

        private byte[] mRecvData;

        ParseWriteResponseRunnable(byte[] recvData) {
            this.mRecvData = recvData;
        }

        @Override
        public void run() {
            mSendNextWriteRequestLock.lock();
            try {
                if (mRecvData.length == LENGTH_WRITE_RESPONSE_PDU) {
                    if (mSendingWriteAttributesRequest != null) {
                        int parseResult = mSendingWriteAttributesRequest.parseResponse(mRecvData);
                        if (parseResult == AttributeParseResult.PARSE_SUCCESS) {
                            mSendNextWriteRequestCondition.signal();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mSendNextWriteRequestLock.unlock();
            }
        }
    }







    public void releaseAdapter() {
        destroyReceiver();
        mStartWorkFlag = false;
        mUsbDataReceiverList.clear();
        mUsbDataReceiverList = null;

        if (mSendWriteCommandThreadPool != null && !mSendWriteCommandThreadPool.isShutdown()) {
            mSendWriteCommandThreadPool.isShutdown();
            mSendWriteCommandThreadPool = null;
        }

        if (mAttRequestPduCacheQueue != null) {
            mAttRequestPduCacheQueue.clear();
            mAttRequestPduCacheQueue = null;
        }
    }


}
