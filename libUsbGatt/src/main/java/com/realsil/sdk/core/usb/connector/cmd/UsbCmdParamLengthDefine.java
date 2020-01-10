package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This file defines the specific length of each parameter in the usb command.
 */
public interface UsbCmdParamLengthDefine {

    /**
     * The length of the Usb Command Opcode Field in usb command message (2 Octets).
     */
    int LENGTH_USB_CMD_OPCODE_FIELD      = 2;
    /**
     * The length of the Parameter Total Len Field in usb command message. (1 Octets)
     */
    int LENGTH_PARAMETER_TOTAL_LEN_FIELD = 1;


}
