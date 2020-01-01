package com.realsil.sdk.core.usb.connector;

import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;

/**
 * A callback method is used to listen the request sent by the client to the server.
 *
 * @author xp.chen
 */
public abstract class BaseRequestCallback {

    /**
     * This callback method will be called when the request is sent successfully.
     */
    public void onSendSuccess() {}

    /**
     * This callback method will be called when the request is sent failed.
     *
     * @param sendResult If sending fails, it will be a negative value.
     */
    public void onSendFailed(int sendResult) {}

    /**
     * This callback method will be called when the request is sent successfully,
     * but an error response pdu is received from the server.
     *
     * @param att_opcode   Opcode of error response, When receiving an error, it is {@link AttPduOpcodeDefine#ERROR_RESPONSE}.
     * @param request_code The request opcode that generated this error response.
     * @param att_handler  The attribute handle that generated this error response.
     * @param error_code   The reason why the request has generated an error response
     */
    public void onReceiveFailed(byte att_opcode, byte request_code, short att_handler, byte error_code) {}

    /**
     * This method will be called when the request is sent,but did not receive a write response from the server within 30s.
     */
    public void onReceiveTimeout() {}

}
