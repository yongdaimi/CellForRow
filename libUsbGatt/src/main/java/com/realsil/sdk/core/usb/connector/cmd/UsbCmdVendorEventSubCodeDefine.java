package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This interface defines the vendor event sub code of the message received from server.
 *
 * @author xp.chen
 */
public interface UsbCmdVendorEventSubCodeDefine {

    /**
     * 0x21 = Dongle Config Code
     */
    byte DONGLE_CONFIG_CODE          = 0x21;
    /**
     * 0x22 = Bluetooth Connected Code
     */
    byte BLUETOOTH_CONNECTED_CODE    = 0x22;
    /**
     * 0x23 = Bluetooth DisConnected Code
     */
    byte BLUETOOTH_DISCONNECTED_CODE = 0x23;

}