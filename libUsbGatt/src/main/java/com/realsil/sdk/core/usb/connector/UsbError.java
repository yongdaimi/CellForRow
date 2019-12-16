package com.realsil.sdk.core.usb.connector;

public class UsbError {

    static final String STR_CONTEXT_IS_NULL                    = "[LocalUsbConnector]: Application context cannot be empty";
    static final String STR_CONTEXT_GET_USB_MANAGER_FAILED     = "[LocalUsbConnector]: Can not get UsbManager";
    static final String STR_CAN_NOT_FOUND_USB_DEVICE           = "[LocalUsbConnector]: Please connect Usb Device";
    static final String STR_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE = "[LocalUsbConnector]: Unable to find the specified USB device";
    static final String STR_DEVICE_IS_NOT_AUTHORIZED           = "[LocalUsbConnector]: Device is not authorized";
    static final String STR_CAN_NOT_FOUND_USB_ENDPOINT         = "[LocalUsbConnector]: Can not find the specified Endpoint";
    static final String STR_OPEN_USB_CONNECTION_FAILED         = "[LocalUsbConnector]: Open Usb device connection failed";
    static final String STR_USB_CONNECTION_NOT_ESTABLISHED     = "[LocalUsbConnector]: Usb connection has not been established";
    static final String STR_HOLD_USB_INTERFACE                 = "[LocalUsbConnector]: Hold Usb interface failed";
    static final String STR_USB_SEND_DATA_FAILED               = "[LocalUsbConnector]: Send data failed";
    static final String STR_USB_RECEIVE_DATA_FAILED            = "[LocalUsbConnector]: Receive data failed";
    static final String STR_USB_RELEASE_INTERFACE_FAILED       = "[LocalUsbConnector]: Failed to release USB interface";
    static final String STR_USB_SEND_BUF_IS_NULL               = "[LocalUsbConnector]: The buff used for sending cannot be empty";

    private static final String STR_UNKNOWN_ERROR = "[LocalUsbConnector]: Unknown Error";

    public static final int CODE_NO_ERROR = 0;

    public static final int CODE_CONTEXT_IS_NULL                    = -100;
    public static final int CODE_CONTEXT_GET_USB_MANAGER_FAILED     = -101;
    public static final int CODE_CAN_NOT_FOUND_USB_DEVICE           = -102;
    public static final int CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE = -103;
    public static final int CODE_DEVICE_IS_NOT_AUTHORIZED           = -104;
    public static final int CODE_CAN_NOT_FOUND_USB_ENDPOINT         = -105;
    public static final int CODE_OPEN_USB_CONNECTION_FAILED         = -106;
    public static final int CODE_USB_CONNECTION_NOT_ESTABLISHED     = -107;
    public static final int CODE_HOLD_USB_INTERFACE                 = -108;
    public static final int CODE_USB_SEND_DATA_FAILED               = -109;
    public static final int CODE_USB_RECEIVE_DATA_FAILED            = -110;
    public static final int CODE_USB_RELEASE_INTERFACE_FAILED       = -111;


    public String err2str(int errorCode) {
        String retstr = null;
        switch (errorCode) {
            case CODE_CONTEXT_IS_NULL:
                retstr = STR_CONTEXT_IS_NULL;
                break;
            case CODE_CONTEXT_GET_USB_MANAGER_FAILED:
                retstr = STR_CONTEXT_GET_USB_MANAGER_FAILED;
                break;
            case CODE_CAN_NOT_FOUND_USB_DEVICE:
                retstr = STR_CAN_NOT_FOUND_USB_DEVICE;
                break;
            case CODE_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE:
                retstr = STR_CAN_NOT_FOUND_SPECIFIED_USB_DEVICE;
                break;
            case CODE_DEVICE_IS_NOT_AUTHORIZED:
                retstr = STR_DEVICE_IS_NOT_AUTHORIZED;
                break;
            case CODE_CAN_NOT_FOUND_USB_ENDPOINT:
                retstr = STR_CAN_NOT_FOUND_USB_ENDPOINT;
                break;
            case CODE_OPEN_USB_CONNECTION_FAILED:
                retstr = STR_OPEN_USB_CONNECTION_FAILED;
                break;
            case CODE_USB_CONNECTION_NOT_ESTABLISHED:
                retstr = STR_USB_CONNECTION_NOT_ESTABLISHED;
                break;
            case CODE_HOLD_USB_INTERFACE:
                retstr = STR_HOLD_USB_INTERFACE;
                break;
            case CODE_USB_SEND_DATA_FAILED:
                retstr = STR_USB_SEND_DATA_FAILED;
                break;
            case CODE_USB_RECEIVE_DATA_FAILED:
                retstr = STR_USB_RECEIVE_DATA_FAILED;
                break;
            case CODE_USB_RELEASE_INTERFACE_FAILED:
                retstr = STR_USB_RELEASE_INTERFACE_FAILED;
                break;
            default:
                retstr = STR_UNKNOWN_ERROR;
                break;
        }
        return retstr;
    }

}
