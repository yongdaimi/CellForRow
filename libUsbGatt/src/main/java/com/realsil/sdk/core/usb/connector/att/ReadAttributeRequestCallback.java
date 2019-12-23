package com.realsil.sdk.core.usb.connector.att;

/**
 * A callback method is used to listen the status that Client read attribute to server.
 *
 * @author xp.chen
 */
public abstract class ReadAttributeRequestCallback {

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest} is sent successfully.
     */
    public void onRequestSendSuccess() {}

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.ReadAttributeRequest} is sent failed.
     *
     * @param sendResult If sending fails, it will be a negative value.
     */
    public void onRequestSendFailed(int sendResult) {}

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest}is sent successfully
     * and also received a write response from the server
     */
    public void onReadSuccess(byte[] attributeValue) {}

    /**
     * This callback method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest} is sent successfully,
     * but an error response pdu is received from the server.
     *
     * @param att_opcode   Opcode of error response, When receiving an error, it is {@link AttributeOpcode#ERROR_RESPONSE}.
     * @param request_code The request opcode that generated this error response.
     * @param att_handler  The attribute handle that generated this error response.
     * @param error_code   The reason why the request has generated an error response
     */
    public void onWriteFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {}

    /**
     * This method will be called when the {@link com.realsil.sdk.core.usb.connector.att.impl.WriteAttributeRequest} is sent,
     * but did not receive a write response from the server within 30s
     */
    public void onWriteTimeout() {}

}
