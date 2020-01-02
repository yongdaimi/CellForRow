package com.realsil.sdk.core.usb.connector.att.callback;

public abstract class WriteAttributeCommandCallback {

    /**
     * This callback method will be called when the write command is sent successfully.
     */
    public void onSendSuccess() {}

    /**
     * This callback method will be called when the write command is sent failed.
     *
     * @param sendResult If sending fails, it will be a negative value.
     */
    public void onSendFailed(int sendResult) {}

}
