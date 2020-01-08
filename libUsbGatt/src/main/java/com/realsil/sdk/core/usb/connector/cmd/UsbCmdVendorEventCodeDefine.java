package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This interface defines the event code of the message received from server.
 *
 * @author xp.chen
 */
public interface UsbCmdVendorEventCodeDefine {

    /**
     * 0x0E = Command Complete Event
     */
    byte COMMAND_COMPLETE_EVENT = 0x0E;
    /**
     * 0xFF = Vendor Custom Event
     */
    byte VENDOR_EVENT           = (byte) 0xFF;

}