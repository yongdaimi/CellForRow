package com.realsil.sdk.core.usb.connector.att.impl;


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
     *
     * @see BaseAttributeProtocol#selectComfortableReportID
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
     * Report id field of send message, It stands for Transparent Transport 1(Support Max Packet Size: 63).
     */
    private static final byte REPORT_ID_16 = 16;

    /**
     * Report id field of send message, It stands for Transparent Transport 2(Support Max Packet Size: 127).
     */
    private static final byte REPORT_ID_17 = 17;

    /**
     * Report id field of send message, It stands for Transparent Transport 3(Support Max Packet Size: 191).
     */
    private static final byte REPORT_ID_18 = 18;

    /**
     * Report id field of send message, It stands for Transparent Transport 4(Support Max Packet Size: 255).
     */
    private static final byte REPORT_ID_19 = 19;

    /**
     * Unknown Report ID.
     */
    private static final byte REPORT_ID_UNKNOWN = -1;

    /**
     * The maximum packet size supported by the Transparent Transport 1
     */
    private static final int MAX_PACKET_SIZE_63 = 63;

    /**
     * The maximum packet size supported by the Transparent Transport 2
     */
    private static final int MAX_PACKET_SIZE_127 = 127;

    /**
     * The maximum packet size supported by the Transparent Transport 3
     */
    private static final int MAX_PACKET_SIZE_191 = 191;

    /**
     * The maximum packet size supported by the Transparent Transport 4
     */
    private static final int MAX_PACKET_SIZE_255 = 255;

    /**
     * Select the appropriate Report ID based on the size of the packets sent.
     *
     * @see BaseAttributeProtocol#mReportID
     */
    static byte selectComfortableReportID(int packageSize) {
        if (packageSize > MAX_PACKET_SIZE_255) {
            return REPORT_ID_UNKNOWN;
        } else if (packageSize > MAX_PACKET_SIZE_191) {
            return REPORT_ID_19;
        } else if (packageSize > MAX_PACKET_SIZE_127) {
            return REPORT_ID_18;
        } else if (packageSize > MAX_PACKET_SIZE_63) {
            return REPORT_ID_17;
        } else {
            return REPORT_ID_16;
        }
    }

    /**
     * Get the data to be sent to the server.
     *
     * @return Data sent to the server.
     */
    public byte[] getSendData() {
        return mSendData;
    }

}

