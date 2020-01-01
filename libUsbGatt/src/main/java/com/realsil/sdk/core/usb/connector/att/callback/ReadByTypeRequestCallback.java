package com.realsil.sdk.core.usb.connector.att.callback;

import com.realsil.sdk.core.usb.connector.BaseRequestCallback;

/**
 * This callback is used to obtain the values of attributes by send {@link com.realsil.sdk.core.usb.connector.att.impl.ReadByTypeRequest}
 *
 * @author xp.chen
 */
public abstract class ReadByTypeRequestCallback extends BaseRequestCallback {

    /**
     * This callback method will be called when received read response from server.
     *
     * @param attributeDataLength The size of each attribute handle value pair
     * @param attributeDataList   A list of attribute data
     */
    public void onReadSuccess(int attributeDataLength, byte[] attributeDataList) {}

}
