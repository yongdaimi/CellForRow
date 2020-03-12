package com.realsil.sdk.core.usb.connector.att.impl;

/**
 * The Write Command is used to request the server to write the value of an
 * attribute, typically into a control-point attribute.
 *
 * @author xp.chen
 */
public class WriteAttributeCommandTest3 extends WriteAttributeCommand {


    public WriteAttributeCommandTest3() {
        super(0, new byte[]{});
    }

    @Override
    public void createCommand() {
        this.mSendData = new byte[]{0x04, 0x04, (byte) 0x90, (byte) 0xFC, 0x01, 0x00};
    }

}
