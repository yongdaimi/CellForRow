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
    /**
     * 0xFC83 = Exchange MTU request.
     */
    short EXCHANGE_MTU_REQUEST           = (short) 0xFC86;
    /**
     * 0xFC20 = HCI Vendor Download Command
     */
    short VENDOR_DOWNLOAD_COMMAND        = (short) 0xFC20;
    /**
     * 0x0001 = Read Local Version Information
     */
    short READ_LOCAL_VERSION_INFORMATION = (short) 0x0001;
    /**
     * 0XFC6D = READ ROM Version
     */
    short VENDOR_READ_ROM_VERSION        = (short) 0xFC6D;

}