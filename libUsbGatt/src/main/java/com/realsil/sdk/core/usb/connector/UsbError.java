package com.realsil.sdk.core.usb.connector;

public interface UsbError {

    int CODE_NO_ERROR = 0;

    int CODE_PARAMS_IS_NULL                     = -100;
    int CODE_CONTEXT_GET_USB_MANAGER_FAILED     = -101;
    int CODE_CAN_NOT_FOUND_USB_DEVICE           = -102;
    int CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE = -103;
    int CODE_DEVICE_IS_NOT_AUTHORIZED           = -104;
    int CODE_CAN_NOT_FOUND_USB_ENDPOINT         = -105;
    int CODE_OPEN_USB_CONNECTION_FAILED         = -106;
    int CODE_USB_CONNECTION_NOT_ESTABLISHED     = -107;
    int CODE_HOLD_USB_INTERFACE                 = -108;
    int CODE_USB_SEND_DATA_FAILED               = -109;
    int CODE_USB_RECEIVE_DATA_FAILED            = -110;
    int CODE_USB_RELEASE_INTERFACE_FAILED       = -111;
    int CODE_CAN_NOT_FOUND_USB_INTERFACE        = -112;
    int CODE_WRITE_REQUEST_TO_QUEUE_FAILED      = -113;
    int CODE_WRITE_REQUEST_WAIT_FAILED          = -114;
    int CODE_WRITE_COMMAND_TO_QUEUE_FAILED      = -115;
    int CODE_WRITE_COMMAND_WAIT_FAILED          = -116;

}
