package com.realsil.sdk.core.usb.connector;

public abstract class BaseRequest {

    public abstract void setRequestOpcode();

    public abstract void setMessageBodyLength();

    public abstract void createRequest();

    public abstract void parseResponse(byte[] responseData);
    
}
