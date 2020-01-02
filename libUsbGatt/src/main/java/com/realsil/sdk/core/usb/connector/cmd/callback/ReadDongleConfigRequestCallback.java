package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.UsbGattCharacteristic;
import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

import java.util.List;

public abstract class ReadDongleConfigRequestCallback extends BaseRequestCallback {

    /**
     * This method will be called when all the information of OTA Characteristic is obtained
     */
    public void onReadOtaCharacteristicList(List<UsbGattCharacteristic> list) {}

    /**
     * This method will be called when read dongle config failed.
     */
    public void onReadFailed() {}

}
