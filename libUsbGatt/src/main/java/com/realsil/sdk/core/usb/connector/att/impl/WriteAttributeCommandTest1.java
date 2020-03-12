package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.BaseRequest;
import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttPduParamLengthDefine;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeCommandCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Write Command is used to request the server to write the value of an
 * attribute, typically into a control-point attribute.
 *
 * @author xp.chen
 */
public class WriteAttributeCommandTest1 extends WriteAttributeCommand {

    public WriteAttributeCommandTest1() {
        super(0, new byte[]{});
    }

    @Override
    public void createCommand() {
        this.mSendData = new byte[]{0x04, 0x13, (byte) 0x89, (byte) 0xFC, 0x10};
    }


}
