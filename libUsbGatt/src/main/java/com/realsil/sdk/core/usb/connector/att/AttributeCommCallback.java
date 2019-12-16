package com.realsil.sdk.core.usb.connector.att;

import android.hardware.usb.UsbEndpoint;

/**
 * Callback interface for listening to att communication.
 *
 * @author xp.chen
 */
public interface AttributeCommCallback {

    /**
     * This method is called when the attribute pdu is sent successfully.
     */
    void onSendSuccess();

    /**
     * This method is called when the attribute pdu fails to send.
     *
     * @param sendResult If sending fails, it will be a negative value.
     * @see android.hardware.usb.UsbDeviceConnection#bulkTransfer(UsbEndpoint, byte[], int, int, int)
     */
    void onSendFailed(int sendResult);

    /**
     * This method is called when the data is sent successfully and the server response is received.
     */
    void onReceiveSuccess();

    /**
     * This method is called when data is sent successfully, but an error response is received from the server.
     *
     * @param att_opcode   Opcode of error response, When receiving an error, it is 0x01.
     * @param request_code The request opcode that generated this error response.
     * @param att_handler  The attribute handle that generated this error response.
     * @param error_code   The reason why the request has generated an error response
     */
    void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code);
}
