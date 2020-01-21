package com.realsil.android.dongle.helper;

import com.realsil.android.dongle.entity.SendPacketInfo;
import com.realsil.android.dongle.util.ByteUtils;
import com.realsil.android.dongle.util.LogX;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.cmd.callback.VendorDownloadCommandCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.VendorDownloadCommand;

import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Use this class to complete specific patch packet sending actions.
 *
 * @author xp.chen
 */
public class DownloadPatchHelper {


    private static final String TAG = DownloadPatchHelper.class.getSimpleName();

    private ArrayBlockingQueue<SendPacketInfo> mSendingQueue;

    private ReadPacketThread mReadPacketThread;
    private SendPacketThread mSendPacketThread;

    private byte[] mPatchCodeArray;
    private int    mPatchCodeTotalLength;

    private static final int MAX_SEND_LENGTH_ONCE  = 200;
    private static final int MAX_LENGTH_SEND_QUEUE = 1;

    private static final int MAX_INDEX_IN_SEND_PACKET = 127;

    private static final int LAST_PACKET_SIGN_BIT_VALUE = 1;

    private volatile boolean mDownloadPatchStarted = false;

    public static final int DOWNLOAD_FAILED_RECEIVE_TIMEOUT = -500;
    public static final int DOWNLOAD_FAILED_RECEIVE_ERROR   = -501;

    private int mSentByteNum = 0;
    private int mSendPercent = 0;

    private final Object mLock = new Object();

    private OnDownloadStatusChangeListener mOnDownloadStatusChangeListener;

    public DownloadPatchHelper(byte[] patchCodeArray) {
        this.mPatchCodeArray = patchCodeArray;
        this.mPatchCodeTotalLength = mPatchCodeArray.length;
        this.mSendingQueue = new ArrayBlockingQueue<>(MAX_LENGTH_SEND_QUEUE);
    }

    public void startDownloadPatch() {
        mDownloadPatchStarted = true;
        if (mReadPacketThread == null) {
            mReadPacketThread = new ReadPacketThread();
            mReadPacketThread.start();
        }
        if (mSendPacketThread == null) {
            mSendPacketThread = new SendPacketThread();
            mSendPacketThread.start();
        }
    }

    public void stopDownloadPatch() {
        if (mReadPacketThread != null) {
            mReadPacketThread.interrupt();
            mReadPacketThread = null;
        }
        if (mSendPacketThread != null) {
            mSendPacketThread.interrupt();
            mSendPacketThread = null;
        }
        mDownloadPatchStarted = false;
    }

    private class ReadPacketThread extends Thread {

        @Override
        public void run() {
            super.run();

            int readOffset = 0;
            int remainLength = mPatchCodeTotalLength;

            while (remainLength > 0 && mDownloadPatchStarted) {
                byte[] sendData;
                SendPacketInfo sendPacketInfo;

                if (remainLength > MAX_SEND_LENGTH_ONCE) {
                    sendData = new byte[MAX_SEND_LENGTH_ONCE];
                    sendPacketInfo = new SendPacketInfo(false, sendData);
                } else {
                    sendData = new byte[remainLength];
                    sendPacketInfo = new SendPacketInfo(true, sendData);
                }

                System.arraycopy(mPatchCodeArray, readOffset, sendData, 0, sendData.length);
                LogX.i(TAG, "extra packet: " + ByteUtils.convertByteArr2String(sendData));

                try {
                    mSendingQueue.put(sendPacketInfo);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                // Update remain length of the patch code array and set a new value to readOffset.
                remainLength -= sendData.length;
                readOffset += sendData.length;

                synchronized (mLock) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            // All packet sent completed if the remainLength is a negative value.
            mDownloadPatchStarted = false;
        }
    }

    private class SendPacketThread extends Thread {

        private AtomicInteger mPacketIndexValue = new AtomicInteger();

        // Number of bytes has sent.
        private int mSentByteNum = 0;

        @Override
        public void run() {
            super.run();
            if (mOnDownloadStatusChangeListener != null) {
                mOnDownloadStatusChangeListener.onDownloadStarted();
            }
            // Start the process of sending packets
            while (mDownloadPatchStarted) {
                // If the index of the sent data packet is greater than 127, reset the index
                // value to 0
                // TODO: 2020/1/14 max value num need to calculate
                if (mPacketIndexValue.intValue() > MAX_INDEX_IN_SEND_PACKET) {
                    mPacketIndexValue.set(0);
                }

                // Extra a data packet from blocking queue.
                try {
                    SendPacketInfo sendPacketInfo = mSendingQueue.take();
                    sendPacketInfo.setSendIndex((byte) mPacketIndexValue.intValue());
                    new VendorDownloadThread(sendPacketInfo).start();
                    // If the data packet is sent, add 1 to the original index value.
                    mPacketIndexValue.incrementAndGet();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (mOnDownloadStatusChangeListener != null) {
                        mOnDownloadStatusChangeListener.onDownloadCanceled();
                    }
                    break;
                }

            }
        }
    }


    private class VendorDownloadThread extends Thread {

        private SendPacketInfo mSendPacketInfo;

        VendorDownloadThread(SendPacketInfo sendPacketInfo) {
            this.mSendPacketInfo = sendPacketInfo;
        }

        @Override
        public void run() {
            super.run();
            // Construct a new VendorDownloadCommand to send the packet.
            VendorDownloadCommand command = new VendorDownloadCommand(mSendPacketInfo.isLastPacket(),
                    mSendPacketInfo.getSendIndex(), mSendPacketInfo.getSendPacketBuff());
            command.addVendorDownloadCommandCallback(new VendorDownloadCommandCallback() {
                @Override
                public void onTransferSuccess(byte packetIndex) {
                    super.onTransferSuccess(packetIndex);
                    // Notify to send next packet
                    synchronized (mLock) {
                        mLock.notify();
                        /*int lastPacketSignBit = (packetIndex & 0x80) >>> 7;
                        // If the highest bit of the received index value is 1,it means that the BT controller
                        // has received all the data packets.
                        if (lastPacketSignBit == LAST_PACKET_SIGN_BIT_VALUE && mOnDownloadStatusChangeListener != null) {
                            reset();
                            mOnDownloadStatusChangeListener.onDownloadCompleted();
                        }*/

                        mSentByteNum += command.getSentDataBlockLength();
                        BigDecimal sentBgNum = new BigDecimal(mSentByteNum);
                        BigDecimal totalBgNum = new BigDecimal(mPatchCodeTotalLength);
                        double decimalPercent = sentBgNum.divide(totalBgNum, 2, BigDecimal.ROUND_DOWN).doubleValue();
                        int downloadPercent = (int) (decimalPercent * 100);
                        if (mOnDownloadStatusChangeListener != null && downloadPercent != mSendPercent) {
                            mSendPercent = downloadPercent;
                            mOnDownloadStatusChangeListener.onDownloadProgressChanged(downloadPercent);
                        }

                    }
                }

                @Override
                public void onTransferFail() {
                    super.onTransferFail();
                    synchronized (mLock) {
                        mLock.notify();
                        // If receive an error, end the entire operation.
                        mDownloadPatchStarted = false;
                        if (mOnDownloadStatusChangeListener != null) {
                            reset();
                            mOnDownloadStatusChangeListener.onDownloadFailed(DOWNLOAD_FAILED_RECEIVE_ERROR);
                        }
                    }
                }

                @Override
                public void onReceiveTimeout() {
                    super.onReceiveTimeout();
                    synchronized (mLock) {
                        mLock.notify();
                        if (mOnDownloadStatusChangeListener != null) {
                            reset();
                            mOnDownloadStatusChangeListener.onDownloadFailed(DOWNLOAD_FAILED_RECEIVE_TIMEOUT);
                        }
                    }
                }

            });
            LocalUsbConnector.getInstance().sendRequest(command);
        }
    }


    public interface OnDownloadStatusChangeListener {
        void onDownloadStarted();

        void onDownloadCanceled();

        void onDownloadFailed(int errorCode);

        void onDownloadCompleted();

        void onDownloadProgressChanged(int progress);
    }

    public void addOnDownloadStatusChangeListener(OnDownloadStatusChangeListener listener) {
        this.mOnDownloadStatusChangeListener = listener;
    }

    private void reset() {
        mReadPacketThread = null;
        mSendPacketThread = null;
        mSentByteNum = 0;
        mSendPercent = 0;
    }

}
