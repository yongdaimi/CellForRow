package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This file defines the specific length of each parameter in the usb command.
 */
public interface UsbCmdParamLengthDefine {

    /**
     * The length of the Usb Command Opcode in usb command message (2 Octets).
     */
    int LENGTH_USB_CMD_OPCODE = 2;

    /**
     * If there are no parameters, set the parameters to 0x00 and the length to 1
     */
    int LENGTH_USB_CMD_NON_PARAM = 1;

}
