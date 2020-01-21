package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * This callback is used to listen for returned results of the
 * {@link com.realsil.sdk.core.usb.connector.cmd.impl.ReadRomVersionCommand}
 *
 * @author xp.chen
 */
public abstract class ReadRomVersionCommandCallback extends BaseRequestCallback {
    /**
     * This method will be called when the rom version information is read.
     */
    public void onReadRomVersionSuccess(int romVersion) {}

    /**
     * This method will be called if no version information is read.
     */
    public void onReadRomVersionFail() {}

}
