package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.UsbConfig;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.VendorDownloadCommandCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use an instance of this class to send packets of usb patch
 */
public class VendorDownloadCommand extends BaseUsbRequest {


    /**
     * bit[7] = 0 of the packet index means to start and continue sending data blocks.
     */
    private static final byte PACKET_INDEX_BYTE_SIGN_BIT_START = 0x7F;
    /**
     * bit[7] = 1 of the packet index means the end of sending the data block, this usually represents the last packet.
     */
    private static final byte PACKET_INDEX_BYTE_SIGN_BIT_LAST  = (byte) 0x80;

    /**
     * Packet index of data block
     */
    private byte mPacketIndex;

    /**
     * The data block will be sent to bt controller.
     */
    private byte[] mDataBlock;

    /**
     * The length of the packet index field.
     */
    private static final int LENGTH_PACKET_INDEX_FIELD = 1;

    /**
     * The length of total parameters. In this class, mParamTotalLength = packetIndex Length + dataBlock Length
     */
    private int mParamTotalLength;


    /**
     * Add a callback to the current request to listen the status of sending and receiving.
     *
     * @param callback Callback for status listening
     */
    public void addVendorDownloadCommandCallback(VendorDownloadCommandCallback callback) {
        this.mBaseRequestCallback = callback;
    }

    /**
     * Get the callback that is currently used to listen the status.
     *
     * @return Callback for status listening
     */
    public VendorDownloadCommandCallback getVendorDownloadCommandCallback() {
        return (VendorDownloadCommandCallback) mBaseRequestCallback;
    }

    /**
     * Get the length of the sent data block.
     *
     * @return length of the sent data block.
     */
    public int getSentDataBlockLength() {
        return mDataBlock != null ? mDataBlock.length : 0;
    }

    /**
     * Use this constructor to build a new Vendor Download Command
     *
     * @param isLastPacket An identifier to specify whether the current packet is the last packet.
     *                     <p>true: If current packet is the last packet.</p>
     *                     <p>false: If current packet is not the last packet.</p>
     * @param packetIndex  The sequence number of the current packet. Note: This sequence number can not be greater than 127
     * @param dataBlock    The real data block(4N Octet).
     *                     <p>The length is the multiple of 4 bytes. If it is not the last block, host may send
     *                     252 byte in data field to reduce the time in download procedure.</p>
     */
    public VendorDownloadCommand(boolean isLastPacket, byte packetIndex, byte[] dataBlock) {
        this.mPacketIndex |= packetIndex;
        if (isLastPacket) {
            this.mPacketIndex |= PACKET_INDEX_BYTE_SIGN_BIT_LAST;
        } else {
            this.mPacketIndex &= PACKET_INDEX_BYTE_SIGN_BIT_START;
        }
        this.mDataBlock = dataBlock;
        this.mParamTotalLength = LENGTH_PACKET_INDEX_FIELD + mDataBlock.length;
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.VENDOR_DOWNLOAD_COMMAND;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE_FIELD
                + UsbCmdParamLengthDefine.LENGTH_PARAMETER_TOTAL_LEN_FIELD + mParamTotalLength;
    }

    @Override
    public void createRequest() {
        super.createRequest();

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID Note: Report ID = 5 in Download Patch in normal mode
        mSendReportID = UsbConfig.REPORT_ID_5;
        byteBuffer.put(mSendReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mSendMessageLength);

        /// Put USB PDU
        // Usb opcode
        byteBuffer.putShort(2, request_opcode);
        // Parameters total length
        byteBuffer.put(4, (byte) mParamTotalLength);
        // Packet index
        byteBuffer.put(5, mPacketIndex);
        // Data block
        System.arraycopy(mDataBlock, 0, mSendData, 6, mDataBlock.length);
    }

    @Override
    public void parseResponse(byte[] responseData) {
        super.parseResponse(responseData);
        if (mReceiveReportID == mSendReportID && response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            byte receivedIndex = responseData[8];
            if (getVendorDownloadCommandCallback() != null) {
                getVendorDownloadCommandCallback().onTransferSuccess(receivedIndex);
            }
        } else {
            if (getVendorDownloadCommandCallback() != null) {
                getVendorDownloadCommandCallback().onTransferFail();
            }
        }
    }

}
