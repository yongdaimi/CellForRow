package com.realsil.sdk.core.usb.connector.cmd.impl;

public abstract class BaseUsbRequest {


    short request_opcode;

    /**
     * The final message data send to the usb.
     */
    byte[] mSendData;

    public abstract void createRequest();

    public abstract void parseResponse();


}
