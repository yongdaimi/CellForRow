package com.realsil.sdk.core.usb.connector;

public interface UsbLogInfo {

    String INFO_GET_USB_DEVICE_SUCCESSFULLY = "[LocalUsbConnector]: The specified device was found, waiting for authorization by user";
    String INFO_DEVICE_IS_AUTHORIZED = "[LocalUsbConnector]: Device is authorized";
    String INFO_FOUND_THE_SPECCIFIED_USB_ENDPOINT = "[LocalUsbConnector]: Found the specified UsbEndpoint";

}
