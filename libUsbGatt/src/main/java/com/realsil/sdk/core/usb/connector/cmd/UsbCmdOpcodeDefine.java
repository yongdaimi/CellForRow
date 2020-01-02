package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This interface defines the opcode of the command sent to the USB
 *
 * @author xp.chen
 */
public interface UsbCmdOpcodeDefine {

    /**
     * 0xFC84 = Query bluetooth connection state request.
     */
    short QUERY_BT_CONN_STATE_REQUEST    = (short) 0xFC84;
    /**
     * 0xFC82 = Read usb dongle config request.
     */
    short READ_USB_DONGLE_CONFIG_REQUEST = (short) 0xFC82;

}