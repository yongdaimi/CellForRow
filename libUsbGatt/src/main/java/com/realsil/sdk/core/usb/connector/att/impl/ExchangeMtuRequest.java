package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttributeOpcode;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Exchange MTU Request is used by client to inform the server of the client's maximum receive
 * MTU size and request the server to respond with its maximum receive MTU size.
 *
 * @author xp.chen
 */
public class ExchangeMtuRequest extends BaseExchangeMtuRequest {


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

    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttributeOpcode.EXCHANGE_MTU_REQUEST;
    }

    @Override
    public void createRequest() {
        this.mAttPduLength = LENGTH_ATT_OPCODE + LENGTH_ATT_HANDLE;
        this.mSendDataLength = LENGTH_WRITE_REQUEST_HEAD + mAttPduLength;
        this.mSendData = new byte[mSendDataLength];
        this.mReportID = selectComfortableReportID(mSendDataLength);

        ByteBuffer byteBuffer = ByteBuffer.wrap(mSendData);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        /// Put Protocol Header
        // ReportID
        byteBuffer.put(mReportID);
        // message length(ATT PDU length)
        byteBuffer.put(1, (byte) mAttPduLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, request_opcode);
        // Client Rx Mtu
        byteBuffer.putShort(3, mMtuSize);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttributeOpcode.EXCHANGE_MTU_RESPONSE) {
            short server_mtu_size = 0;
            if (response.length > 1) {
                ByteBuffer buffer = ByteBuffer.wrap(response);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                server_mtu_size = buffer.getShort(1);
            }
            if (getExchangeMtuRequestCallback() != null) {
                getExchangeMtuRequestCallback().onReceiveServerRxMtu(server_mtu_size & 0x0FF);
            }
            mParseResult = AttributeParseResult.PARSE_SUCCESS;
        } else {
            if (getExchangeMtuRequestCallback() != null)
                getExchangeMtuRequestCallback().onReceiveFailed(response_opcode, error_request_opcode, error_att_handle, error_code);
        }
    }

}
