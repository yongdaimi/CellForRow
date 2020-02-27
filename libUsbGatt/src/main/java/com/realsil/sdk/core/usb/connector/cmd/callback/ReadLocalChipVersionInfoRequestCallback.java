package com.realsil.sdk.core.usb.connector.cmd.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * A callback class is used to listen the sending and receiving status
 * of the {@link com.realsil.sdk.core.usb.connector.cmd.impl.ReadLocalChipVersionInfoRequest}
 *
 * @author xp.chen
 */
public abstract class ReadLocalChipVersionInfoRequestCallback extends BaseRequestCallback {


    /**
     * This method will be called if the version information is received.
     *
     * @param hciVersion       HCI Version
     * @param hciRevision      HCI Revision
     * @param lmpVersion       LMP Version
     * @param lmpSubVersion    Manufacturer Name
     * @param manufacturerName LMP Subversion
     */
    public void onReceivedVersionInformation(int hciVersion, int hciRevision, int lmpVersion,
                                             int lmpSubVersion, int manufacturerName) {}

    /**
     * This method will be called if an error occurs while receiving data.
     */
    public void onReceiveFailed() {}
}
