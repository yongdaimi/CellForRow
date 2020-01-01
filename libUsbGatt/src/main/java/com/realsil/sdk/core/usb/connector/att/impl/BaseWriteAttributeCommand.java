package com.realsil.sdk.core.usb.connector.att.impl;

/**
 * An abstract class template for creating Write Attribute PDUs Command
 *
 * @author xp.chen
 */
abstract class BaseWriteAttributeCommand {

    /**
     * The handler of the attribute to be written or read.
     */
    short mAttHandle;

    /**
     * The value of the attribute to be written.
     */
    byte[] mAttValue;

    /**
     * Length of ATT PDU in send data.
     * <p>Note: It just represent the length of the ATT PDU, Excluding report ID and its own length.
     * AttPduLength = Att opcode Length(1B) + Att handle Length(2B) + att value(0 to ATT_MTU-3) </p>
     */
    int mAttPduLength;

    /**
     * Length of the final data send to the server.
     * <p>sendDataLength = ReportIDLength(1B) + MessageLength(1B) + ATT PDU Length </p>
     */
    int mSendDataLength;

    /**
     * The final message data send to the server.
     */
    byte[] mSendData;

    /**
     * Report ID of this message.
     */
    byte mReportID;

    /**
     * Use this method to create a Write Attributes Command.
     */
    public abstract void createCommand();

    /**
     * Get the data to be sent to the server.
     *
     * @return Data sent to the server.
     */
    public byte[] getSendData() {
        return mSendData;
    }

}
