package com.realsil.sdk.core.usb.connector;

public interface UsbDataReceiver {

    void onReceiveUsbData(byte[] receiveData, int dataLength);

}
