package com.realsil.sdk.core.usb.connector;

/**
 * @author xp.chen
 */
public interface UsbHidDefine {

    /**
     * request type for this transaction
     */
    int CONTROL_REQUEST_TYPE    = 0xC0;
    /**
     * request ID for this transaction
     */
    int CONTROL_REQUEST_ID      = 0x01;
    /**
     * value field for this transaction
     */
    int CONTROL_REQUEST_VALUE   = 0x00;
    /**
     * index field for this transaction
     */
    int CONTROL_REQUEST_INDEX   = 0x00;
    /**
     * timeout in milliseconds
     */
    int CONTROL_REQUEST_TIMEOUT = 1;

}
