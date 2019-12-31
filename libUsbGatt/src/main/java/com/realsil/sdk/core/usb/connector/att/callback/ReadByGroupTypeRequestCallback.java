package com.realsil.sdk.core.usb.connector.att.callback;

/**
 * This callback is used to obtain the values of attributes by send {@link com.realsil.sdk.core.usb.connector.att.impl.ReadByGroupTypeRequest}
 *
 * @author xp.chen
 */
public class ReadByGroupTypeRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when received read response from server.
     *
     * @param attributeDataLength The size of each attribute data.
     * @param attributeDataList   A list of attribute data
     */
    public void onReadSuccess(int attributeDataLength, byte[] attributeDataList) {}

}
