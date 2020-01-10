package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * Use this callback to listen to the results returned by the bt controller when sending
 * packets for the download patch operation
 *
 * @author xp.chen
 */
public abstract class VendorDownloadCommandCallback extends BaseRequestCallback {

    /**
     * This method will be called when a data block is successfully received by the bt controller.
     *
     * @param packetIndex The index of the packet what were sent successfully
     */
    public void onTransferSuccess(byte packetIndex) {}

    /**
     * This method will be called if the current packet fails to be sent.
     */
    public void onTransferFail() {}

}
