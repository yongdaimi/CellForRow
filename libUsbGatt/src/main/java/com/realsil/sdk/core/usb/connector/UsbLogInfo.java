package com.realsil.sdk.core.usb.connector;

import java.util.Locale;

final class UsbLogInfo {

    private UsbLogInfo() {}

    private static final String LOG_TITLE     = LocalUsbConnector.class.getSimpleName();
    private static final String LOG_SEPARATOR = "->";

    final static String TYPE_INIT_USB_CONNECTOR   = "Init Usb Connector";
    final static String TYPE_SEND_WRITE_REQUEST   = "Send Write Request";
    final static String TYPE_SEND_WRITE_COMMAND   = "Send Write Command";
    final static String TYPE_SEND_READ_REQUEST    = "Send Read Request";
    final static String TYPE_EXCHANGE_MTU_REQUEST = "Exchange MTU Request";
    final static String TYPE_RUNNING_TIPS         = "Running Tips";
    final static String TYPE_CALL_CONNECT         = "Call Connect";
    final static String TYPE_CALL_DISCONNECT      = "Call Disconnect";
    final static String TYPE_UNKNOWN_INFO_TYPE    = "Unknown Info Type";

    static String msg(String infoType, String infoContent) {
        return String.format(Locale.getDefault(), "%s: [%s] %s %s", LOG_TITLE, infoType, LOG_SEPARATOR, infoContent);
    }


}
