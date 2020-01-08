package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Use Vendor Download Command to complete the function of download patch.
 */
public class VendorDownloadCommand extends BaseUsbRequest {


    /**
     * bit[7] = 0 is tag of the start/continuous data block
     */
    private static final byte PACKET_INDEX_START_TAG = 0x7F;
    /**
     * bit[7] = 1 is tag of the last data block
     */
    private static final byte PACKET_INDEX_LAST_TAG  = (byte) 0x80;

    /**
     * Packet index of data block
     */
    private byte mPacketIndex;

    /**
     * The real data block
     */
    private byte[] mDataBlock;

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
            this.mPacketIndex |= PACKET_INDEX_LAST_TAG;
        } else {
            this.mPacketIndex &= PACKET_INDEX_START_TAG;
        }
    }

    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.VENDOR_DOWNLOAD_COMMAND;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE + UsbCmdParamLengthDefine.LENGTH_USB_CMD_NON_PARAM;
    }

    @Override
    public void createRequest() {
        super.createRequest();

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mSendMessageLength);

        /// Put USB PDU
        // Usb opcode
        byteBuffer.putShort(2, request_opcode);
    }

    @Override
    public void parseResponse(byte[] responseData) {
        super.parseResponse(responseData);
        if (response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            /*byte connectStatus = responseData[8];
            if (getQueryBTConnectStateRequestCallback() != null) {
                getQueryBTConnectStateRequestCallback().onReceiveConnectState(STATUS_SUCCESS,
                        connectStatus == BT_HAS_CONNECTED ? UsbGatt.STATE_CONNECTED : UsbGatt.STATE_DISCONNECTED);
            }*/
        } else {
            /*if (getQueryBTConnectStateRequestCallback() != null) {
                getQueryBTConnectStateRequestCallback().onReceiveConnectState(status_code, BT_HAS_DISCONNECTED);
            }*/
        }
    }

}
