package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.BaseRequest;
import com.realsil.sdk.core.usb.connector.att.AttPduParamLengthDefine;
import com.realsil.sdk.core.usb.connector.att.callback.WriteAttributeCommandCallback;

/**
 * The Write Command is used to request the server to write the value of an
 * attribute, typically into a control-point attribute.
 *
 * @author xp.chen
 */
public class WriteAttributeCommandTest2 extends WriteAttributeCommand {


    public WriteAttributeCommandTest2() {
        super(0, new byte[]{});
    }

    @Override
    public void createCommand() {
        this.mSendData = new byte[]{0x04, 0x04, (byte) 0x90, (byte) 0xFC, 0x01, 0x01};
    }


}
