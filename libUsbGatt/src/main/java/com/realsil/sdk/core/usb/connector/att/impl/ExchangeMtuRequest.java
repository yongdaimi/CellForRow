package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttPduOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttPduParamLengthDefine;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.callback.ExchangeMtuRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Exchange MTU Request is used by client to inform the server of the client's maximum receive
 * MTU size and request the server to respond with its maximum receive MTU size.
 *
 * @author xp.chen
 */
public class ExchangeMtuRequest extends BaseAttributeRequest {


    private short mMtuSize;


    /**
     * Use this constructor to construct a Exchange Mtu Request.
     *
     * @param receiveMtuSize client's maximum receive MTU size.
     */
    public ExchangeMtuRequest(int receiveMtuSize) {
        if (receiveMtuSize > 0) {
            this.mMtuSize = (short) receiveMtuSize;
        } else {
            throw new IllegalArgumentException("The parameter receiveMtuSize can not be a negative value or zero");
        }
    }

    /**
     * Add a callback interface to listen the status when the client send a {@link ExchangeMtuRequest} to server.
     *
     * @param exchangeMtuRequestCallback A callback is used to listen the status when the client sends a exchange MTU request to server.
     */
    public void addExchangeMtuRequestCallback(ExchangeMtuRequestCallback exchangeMtuRequestCallback) {
        this.mBaseRequestCallback = exchangeMtuRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link ExchangeMtuRequestCallback}.
     *
     * @return A Callback currently for listening to {@link ExchangeMtuRequestCallback}.
     */
    public ExchangeMtuRequestCallback getExchangeMtuRequestCallback() {
        return (ExchangeMtuRequestCallback) mBaseRequestCallback;
    }


    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttPduOpcodeDefine.EXCHANGE_MTU_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = AttPduParamLengthDefine.LENGTH_ATT_OPCODE + AttPduParamLengthDefine.LENGTH_ATT_CLIENT_RX_MTU;
    }

    @Override
    public void createRequest() {
        super.createRequest();

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mSendMessageLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, request_opcode);
        // Client Rx Mtu
        byteBuffer.putShort(3, mMtuSize);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttPduOpcodeDefine.EXCHANGE_MTU_RESPONSE) {
            short server_mtu_size = 0;
            ByteBuffer buffer = ByteBuffer.wrap(response);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            server_mtu_size = buffer.getShort(3);

            if (getExchangeMtuRequestCallback() != null) {
                getExchangeMtuRequestCallback().onReceiveServerRxMtu(server_mtu_size & 0x0FF);
            }
            mParseResult = AttributeParseResult.PARSE_SUCCESS;
        }
    }


}
