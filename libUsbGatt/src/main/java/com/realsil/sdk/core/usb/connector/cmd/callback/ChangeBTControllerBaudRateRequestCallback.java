package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * This callback is used to listen the modified result of baud rate
 *
 * @author xp.chen
 */
public abstract class ChangeBTControllerBaudRateRequestCallback extends BaseRequestCallback {

    /**
     * This method will be called when the baud rate has been modified successfully.
     */
    public void onModifySuccess() {}

    /**
     * This method will be called when the baud rate fail to modify.
     */
    public void onModifyFailed() {}

}
