package com.realsil.sdk.core.usb.connector;

import java.util.Locale;

final class UsbLogInfo {

    private UsbLogInfo(){}

    private static final String LOG_TITLE     = LocalUsbConnector.class.getSimpleName();
    private static final String LOG_SEPARATOR = "->";

    static String TYPE_INIT_USB_CONNECTOR = "Init Usb Connector";
    static String TYPE_SEND_WRITE_REQUEST = "Send Write Request";
    static String TYPE_SEND_WRITE_COMMAND = "Send Write Command";
    static String TYPE_SEND_READ_REQUEST = "Send Read Request";
    static String TYPE_RUNNING_TIPS       = "Running Tips";
    static String TYPE_CALL_CONNECT       = "Call Connect";
    static String TYPE_CALL_DISCONNECT    = "Call Disconnect";


    static String msg(String infoType, String infoContent) {
        return String.format(Locale.getDefault(), "%s: [%s] %s %s", LOG_TITLE, infoType, LOG_SEPARATOR, infoContent);
    }


}
