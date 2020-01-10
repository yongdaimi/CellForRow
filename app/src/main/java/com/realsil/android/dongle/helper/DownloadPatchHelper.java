package com.realsil.android.dongle.helper;

import com.realsil.android.dongle.util.ByteUtils;
import com.realsil.android.dongle.util.LogX;
import com.realsil.sdk.core.usb.connector.LocalUsbConnector;
import com.realsil.sdk.core.usb.connector.cmd.callback.VendorDownloadCommandCallback;
import com.realsil.sdk.core.usb.connector.cmd.impl.VendorDownloadCommand;

import java.math.BigDecimal;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Use this class to complete specific patch packet sending actions.
 *
 * @author xp.chen
 */
public class DownloadPatchHelper {


    private static final String TAG = DownloadPatchHelper.class.getSimpleName();

    private ArrayBlockingQueue<byte[]> mSendingQueue;

    private ReentrantLock mReentrantLock = new ReentrantLock();

    private Condition mSendNextPacketCondition = mReentrantLock.newCondition();

    private ReadPacketThread mReadPacketThread;
    private SendPacketThread mSendPacketThread;

    private byte[] mPatchCodeArray;
    private int    mPatchCodeTotalLength;

    private static final int MAX_READ_LENGTH_ONCE  = 200;
    private static final int MAX_LENGTH_SEND_QUEUE = 1;

    private static final int MAX_SEQUENCE_NUMBER_IN_PACKET_INDEX = 127;

    private static final int LAST_PACKET_SIGN_BIT_VALUE = 1;

    private volatile boolean mStartSendNextPacket = false;

    private OnDownloadStatusChangeListener mOnDownloadStatusChangeListener;

    public DownloadPatchHelper(byte[] patchCodeArray) {
        this.mPatchCodeArray = patchCodeArray;
        this.mPatchCodeTotalLength = mPatchCodeArray.length;
        this.mSendingQueue = new ArrayBlockingQueue<>(MAX_LENGTH_SEND_QUEUE);
    }

    public void startDownloadPatch() {
        mStartSendNextPacket = true;
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
        mStartSendNextPacket = false;
    }

    private class ReadPacketThread extends Thread {

        @Override
        public void run() {
            super.run();

            int readOffset = 0;
            int remainLength = mPatchCodeTotalLength;

            while (remainLength > 0 && mStartSendNextPacket) {

                byte[] sendPacket = null;
                if (remainLength > MAX_READ_LENGTH_ONCE) {
                    sendPacket = new byte[MAX_READ_LENGTH_ONCE];
                } else {
                    sendPacket = new byte[remainLength];
                }

                System.arraycopy(mPatchCodeArray, readOffset, sendPacket, 0, sendPacket.length);
                LogX.i(TAG, "extra packet: " + ByteUtils.convertByteArr2String(sendPacket));

                try {
                    mSendingQueue.put(sendPacket);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                // Update remain length of the patch code array and set a new value to readOffset.
                remainLength -= sendPacket.length;
                readOffset += sendPacket.length;

            }

            // All packet sent completed
            mStartSendNextPacket = false;
        }
    }

    private class SendPacketThread extends Thread {

        private AtomicInteger mAtomicInteger = new AtomicInteger();

        // Number of bytes has sent.
        private int mSentByteNums = 0;

        @Override
        public void run() {
            super.run();
            if (mOnDownloadStatusChangeListener != null) {
                mOnDownloadStatusChangeListener.onDownloadStarted();
            }
            // Start the process of sending packets
            while (mStartSendNextPacket) {
                // If the index of the sent data packet is greater than 127, reset the index
                // value to 0
                if (mAtomicInteger.intValue() > MAX_SEQUENCE_NUMBER_IN_PACKET_INDEX) {
                    mAtomicInteger.set(0);
                }

                // Extra a data packet from blocking queue.
                byte[] sendPacket = null;
                try {
                    sendPacket = mSendingQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (mOnDownloadStatusChangeListener != null) {
                        mOnDownloadStatusChangeListener.onDownloadCanceled();
                    }
                    break;
                }

                // If the length of the sent packet is not equal to 252, it means that it is the
                // last data packet.
                boolean isLastPacket = sendPacket.length != MAX_READ_LENGTH_ONCE;

                // Construct a new VendorDownloadCommand to send the packet.
                VendorDownloadCommand command = new VendorDownloadCommand(isLastPacket,
                        (byte) mAtomicInteger.intValue(), sendPacket);
                command.addVendorDownloadCommandCallback(new VendorDownloadCommandCallback() {
                    @Override
                    public void onTransferSuccess(byte packetIndex) {
                        super.onTransferSuccess(packetIndex);
                        // Notify to send next packet
                        mReentrantLock.lock();
                        mSendNextPacketCondition.notify();
                        mReentrantLock.unlock();

                        int lastPacketSignBit = (packetIndex & 0x80) >>> 7;
                        // If the highest bit of the received index value is 1,it means that the BT controller
                        // has received all the data packets.
                        if (lastPacketSignBit == LAST_PACKET_SIGN_BIT_VALUE && mOnDownloadStatusChangeListener != null) {
                            mOnDownloadStatusChangeListener.onDownloadCompleted();
                        }

                        mSentByteNums += command.getSentDataBlockLength();
                        BigDecimal bg = new BigDecimal((float)mSentByteNums / (float) mPatchCodeTotalLength);
                        float decimalPercent = bg.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
                        int IntegerPercent = (int) (decimalPercent * 100);
                        if (mOnDownloadStatusChangeListener != null) {
                            mOnDownloadStatusChangeListener.onDownloadProgressChanged(IntegerPercent);
                        }
                    }

                    @Override
                    public void onTransferFail() {
                        super.onTransferFail();
                        // If receive an error, end the entire operation.
                        mReentrantLock.lock();
                        mStartSendNextPacket = false;
                        mSendNextPacketCondition.notify();
                        mReentrantLock.unlock();
                        if (mOnDownloadStatusChangeListener != null) {
                            mOnDownloadStatusChangeListener.onDownloadFailed();
                        }
                    }

                });
                LocalUsbConnector.getInstance().sendRequest(command);

                // If one packet is sent, the next packet cannot be sent directly. Need to wait
                // for the result of the previous packet.
                mReentrantLock.lock();
                try {
                    mSendNextPacketCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    if (mOnDownloadStatusChangeListener != null) {
                        mOnDownloadStatusChangeListener.onDownloadCanceled();
                    }
                    break;
                } finally {
                    mReentrantLock.unlock();
                }

                // If the result of the previous packet is received, then update the sequence number
                // of the packet next sent
                mAtomicInteger.incrementAndGet();
            }
        }
    }


    public interface OnDownloadStatusChangeListener {
        void onDownloadStarted();

        void onDownloadCanceled();

        void onDownloadFailed();

        void onDownloadCompleted();

        void onDownloadProgressChanged(int progress);
    }


    public void setOnDownloadStatusChangeListener(OnDownloadStatusChangeListener listener) {
        this.mOnDownloadStatusChangeListener = listener;
    }

}
