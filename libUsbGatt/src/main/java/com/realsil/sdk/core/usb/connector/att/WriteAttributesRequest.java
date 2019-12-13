package com.realsil.sdk.core.usb.connector.att;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Write Request is used to request the server to write the value of an
 * attribute and acknowledge that this has been achieved in a Write Response.
 * @author xp.chen
 */
public class WriteAttributesRequest extends BaseWriteAttributesRequest {


    /**
     * Use this constructor to create a Write Attributes Request.
     *
     * @param attHandle The handler of the attribute to be written.
     * @param attValue The value to be written to the attribute.
     */
    public WriteAttributesRequest(short attHandle, byte[] attValue) {
        this.mAttHandle = attHandle;
        this.mAttValue = attValue;
    }

    @Override
    public void createRequest() {
        this.mAttPduLength = LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE + mAttValue.length;
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mAttPduLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = selectComfortableReportID(mSendDataLength);

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mAttPduLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, OPCODE_WRITE_REQUEST);
        // Att handle
        byteBuffer.putShort(3, mAttHandle);
        // Att value
        System.arraycopy(mAttValue, 0, mSendData, 5, mAttValue.length);
    }

    @Override
    public void parseResponse(byte[] response) {

    }

}
