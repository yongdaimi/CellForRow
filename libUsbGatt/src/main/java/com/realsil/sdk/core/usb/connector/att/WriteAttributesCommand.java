package com.realsil.sdk.core.usb.connector.att;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Write Command is used to request the server to write the value of an
 * attribute, typically into a control-point attribute.
 * @author xp.chen
 */
public class WriteAttributesCommand extends BaseWriteAttributesCommand {


    /**
     * Use this constructor to create a Write Attributes Command.
     *
     * @param attHandle The handler of the attribute to be written.
     * @param attValue The value to be written to the attribute.
     */
    public WriteAttributesCommand(short attHandle, byte[] attValue) {
        this.mAttHandle = attHandle;
        this.mAttValue = attValue;
    }


    @Override
    public void createCommand() {
        this.mAttPduLength = LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE + mAttValue.length;
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mAttPduLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = selectComfortableReportID(mSendDataLength);

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /* Put Protocol Header */
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mAttPduLength);

        /* Put Att PDU */
        // Att opcode
        byteBuffer.put(2, OPCODE_WRITE_COMMAND);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
        // Att value
        System.arraycopy(mAttValue, 0, mSendData, 5, mAttValue.length);
    }


}
