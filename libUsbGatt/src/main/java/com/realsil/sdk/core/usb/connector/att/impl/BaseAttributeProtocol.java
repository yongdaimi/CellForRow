package com.realsil.sdk.core.usb.connector.att.impl;


import com.realsil.sdk.core.usb.connector.UsbConfig;

/**
 * An abstract class template for creating Write Attribute PDUs
 *
 * @author xp.chen
 */
abstract class BaseAttributeProtocol {


    /**
     * The length of the report id in the sent message (1 Octets).
     */
    private static final int LENGTH_REPORT_ID = 1;

    /**
     * The length of the sent message (1 Octets).
     * Note: Does not include the field itself and Report ID.
     */
    private static final int LENGTH_SEND_MESSAGE = 1;

    /**
     * The length of the ATT Opcode in ATT PDU (1 Octets).
     */
    static final int LENGTH_ATT_OPCODE = 1;


    /**
     * The length of the ATT Handle in ATT PDU (2 Octets).
     */
    static final int LENGTH_ATT_HANDLE = 2;


    /**
     * Total length of write request header(LENGTH_REPORT_ID + LENGTH_SEND_MESSAGE + LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE).
     */
    static final int LENGTH_WRITE_REQUEST_HEAD = LENGTH_REPORT_ID + LENGTH_SEND_MESSAGE;

    /**
     * Transmission port ID of the data to be transmitted
     * <p>Need to be determined based on the length of the sent data</p>
     */
    byte mReportID;

    /**
     * The handler of the attribute to be written(Write Request) or set(Write Command).
     */
    short mAttHandle;


    /**
     * The value to be written to the attribute
     */
    byte[] mAttValue;


    /**
     * The final message data send to the server.
     */
    byte[] mSendData;


    /**
     * Length of the final data send to the server.
     * <p>sendDataLength = ReportIDLength(1B) + MessageLength(1B) + ATT PDU Length {@link BaseAttributeProtocol#mAttPduLength}</p>
     */
    int mSendDataLength;


    /**
     * Length of ATT PDU in send data.
     *
     * <p>Note: It just represent the length of the ATT PDU, Excluding report ID and its own length.
     * AttPduLength = Att opcode Length(1B) + Att handle Length(2B) + att value(0 to ATT_MTU-3) </p>
     */
    int mAttPduLength;


    /**
     * Get the data to be sent to the server.
     *
     * @return Data sent to the server.
     */
    public byte[] getSendData() {
        return mSendData;
    }

    /**
     * Select the appropriate Report ID based on the size of the packets sent.
     */
    byte selectComfortableReportID(int packageSize) {
        return UsbConfig.selectComfortableReportID(packageSize);
    }

}

