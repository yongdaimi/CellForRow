package com.realsil.sdk.core.usb.connector.cmd;

/**
 * This interface defines the opcode of the command sent to the USB
 *
 * @author xp.chen
 */
public interface UsbCmdOpcodeDefine {

    /**
     * 0xFC84 = Query bluetooth connection state.
     */
    short QUERY_BT_CONN_STATE = (short) 0xFC84;

}