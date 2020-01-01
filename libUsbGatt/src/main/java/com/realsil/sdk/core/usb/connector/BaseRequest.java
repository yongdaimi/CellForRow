package com.realsil.sdk.core.usb.connector;

public abstract class BaseRequest {


    /**
     * The length of the report id field in the sent message (1 Octets).
     */
    private static final int LENGTH_REPORT_ID_FIELD   = 1;
    /**
     * The length of the message len field (1 Octets).
     * Note: Does not include the field itself and Report ID.
     */
    private static final int LENGTH_MESSAGE_LEN_FIELD = 1;

    /**
     * Total length of write request header(LENGTH_REPORT_ID_FIELD + LENGTH_MESSAGE_LEN_FIELD + LENGTH_ATT_OPCODE + Content [ATT_HANDLE or others, etc]).
     */
    public static final int LENGTH_WRITE_REQUEST_HEAD = LENGTH_REPORT_ID_FIELD + LENGTH_MESSAGE_LEN_FIELD;

    /**
     * The true length of the message.
     * <p> If the request is {@link com.realsil.sdk.core.usb.connector.att.impl.BaseAttributeRequest}, the length is ATT PDU Length </p>
     * <p> If the request is {@link com.realsil.sdk.core.usb.connector.cmd.impl.BaseUsbRequest}, the length is Cmd hdr Length </p>
     *
     * <p>Note: If current request is {@link com.realsil.sdk.core.usb.connector.att.impl.BaseAttributeRequest}, the message length just
     * represent the length of the ATT PDU, Excluding report ID and its own length. For example, if the ATT PDU is Write Attribute Request, then
     * AttPduLength = Att opcode Length(1B) + Att handle Length(2B) + att value(0 to ATT_MTU-3) </p>
     *
     * @see com.realsil.sdk.core.usb.connector.att.impl.BaseAttributeRequest
     * @see com.realsil.sdk.core.usb.connector.cmd.impl.BaseUsbRequest
     */
    protected int mMessageLength;

    /**
     * Call this method to set internal opcode code member variables.
     * <p>The opcode will be 2 octet if the request is {@link com.realsil.sdk.core.usb.connector.cmd.impl.BaseUsbRequest}</p>
     * <p>The opcode will be 1 octet if the request is {@link com.realsil.sdk.core.usb.connector.att.impl.BaseAttributeRequest}</p>
     */
    public abstract void setRequestOpcode();

    /**
     * Call this method to set internal {@link BaseRequest#mMessageLength} member variables.
     */
    public abstract void setMessageLength();

    /**
     * Call this method to create a real request message.
     */
    public abstract void createRequest();

    /**
     * Call this method to parse the data returned by the server
     *
     * @param responseData the data returned by server.
     */
    public abstract void parseResponse(byte[] responseData);

    /**
     * Transmission port ID of the data to be transmitted
     * <p>Need to be determined based on the length of the sent data</p>
     */
    protected byte mReportID;

    /**
     * The final message data send to the server.
     */
    protected byte[] mSendData;

    /**
     * Length of the final data send to the server.
     */
    protected int mSendDataLength;

    /**
     * Get the data to be sent to the server.
     *
     * @return Data sent to the server.
     */
    public byte[] getSendData() {
        return mSendData;
    }

    /**
     * A callback is used to listen the data sending status when the client sends request data to the server.
     */
    protected BaseRequestCallback mBaseRequestCallback;

    /**
     * Get the callback currently used to listen for {@link BaseRequestCallback}.
     *
     * @return A Callback currently for listening to {@link BaseRequestCallback}.
     */
    public BaseRequestCallback getRequestCallback() {
        return mBaseRequestCallback;
    }


    /**
     * Select the appropriate Report ID based on the size of the packets sent.
     */
    public static byte selectComfortableReportID(int packageSize) {
        return UsbConfig.selectComfortableReportID(packageSize);
    }

}
