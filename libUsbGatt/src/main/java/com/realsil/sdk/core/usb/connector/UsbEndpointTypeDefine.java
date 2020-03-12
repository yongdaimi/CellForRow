package com.realsil.sdk.core.usb.connector;

/**
 * @author xp.chen
 */
public interface UsbEndpointTypeDefine {

    int USB_ENDPOINT_NONE = 0;

    int USB_ENDPOINT_BULK_IN      = 100;
    int USB_ENDPOINT_INTERRUPT_IN = 101;
    int USB_ENDPOINT_CONTROL_IN   = 102;

    int USB_ENDPOINT_BULK_OUT      = 200;
    int USB_ENDPOINT_INTERRUPT_OUT = 201;
    int USB_ENDPOINT_CONTROL_OUT   = 202;
}
