package com.realsil.sdk.core.usb.connector.att.impl;

/**
 * This write command is to enable control endpoint support
 * @author xp.chen
 */
public class WriteAttributeCommandTest4 extends WriteAttributeCommand {


    public WriteAttributeCommandTest4() {
        super(0, new byte[]{});
    }

    @Override
    public void createCommand() {
        this.mSendData = new byte[]{0x04, 0x04, (byte) 0x89, (byte) 0xFC, 0x01, 0x01};
    }
    
}
