package com.realsil.sdk.core.usb.connector.att.impl;

import com.realsil.sdk.core.usb.connector.att.AttributeOpcodeDefine;
import com.realsil.sdk.core.usb.connector.att.AttributeParseResult;
import com.realsil.sdk.core.usb.connector.att.callback.ReadByTypeRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The Read By Type Request is used to obtain the values of attributes when the attribute type is known
 * but the handle is not known.
 *
 * @author xp.chen
 */
public class ReadByTypeRequest extends BaseAttributeRequest {

    /**
     * First requested handle number
     */
    private short mStartingAttHandle;

    /**
     * ast requested handle number
     */
    private short mEndingAttHandle;

    /**
     * 16 octet UUID
     */
    private int mAttributeTypeIn16;

    /**
     * 2 octet UUID
     */
    private short mAttributeTypeIn2;

    /**
     * Default starting attribute handle
     */
    private static final int DEFAULT_START_ATT_HANDLE = 0X0001;

    /**
     * Default ending attribute handle
     */
    private static final int DEFAULT_END_ATT_HANDLE = 0xFFFF;


    /**
     * Use this constructor to create a Read By Type Request
     * <p>Note: The difference from the {@link ReadByTypeRequest#ReadByTypeRequest(int, int, short)} method is
     * it can be used to search all attributes between start handle is 0x0001 and end handle is 0xFFFF.
     * </p>
     * <p>
     * attGroupType is defined in {@link com.realsil.sdk.core.usb.connector.att.AttributeTypeDefine}
     * </p>
     *
     * @param attributeType 2 or 16 octet UUID
     * @see com.realsil.sdk.core.usb.connector.att.AttributeTypeDefine
     */
    public ReadByTypeRequest(short attributeType) {
        this(DEFAULT_START_ATT_HANDLE, DEFAULT_END_ATT_HANDLE, attributeType);
    }


    /**
     * Use this constructor to create a Read By Group Type Request.
     *
     * <p>
     * attGroupType is defined in {@link com.realsil.sdk.core.usb.connector.att.AttributeTypeDefine}
     * </p>
     *
     * @param startingAttHandle First requested handle number
     * @param endingAttHandle   Last requested handle number
     * @param attributeType     2 or 16 octet UUID
     * @see com.realsil.sdk.core.usb.connector.att.AttributeTypeDefine
     */
    public ReadByTypeRequest(int startingAttHandle, int endingAttHandle, short attributeType) {
        this.mStartingAttHandle = (short) startingAttHandle;
        this.mEndingAttHandle = (short) endingAttHandle;
        this.mAttributeTypeIn2 = attributeType;
    }

    /**
     * Add a callback interface to listen the status when the client send a {@link ReadByTypeRequest} to server.
     *
     * @param readByTypeRequestCallback callback instance
     */
    public void addReadByTypeRequestCallback(ReadByTypeRequestCallback readByTypeRequestCallback) {
        this.mBaseRequestCallback = readByTypeRequestCallback;
    }

    /**
     * Get the callback currently used to listen fro {@link ReadByTypeRequest}
     *
     * @return A Callback currently for listening to {@link ReadByTypeRequest}.
     */
    public ReadByTypeRequestCallback getReadByTypeRequestCallback() {
        return (ReadByTypeRequestCallback) mBaseRequestCallback;
    }


    @Override
    public void setRequestOpcode() {
        this.request_opcode = AttributeOpcodeDefine.READ_BY_TYPE_REQUEST;
    }

    @Override
    public void setAttPduLength() {
        this.mAttPduLength = LENGTH_ATT_OPCODE_FIELD + LENGTH_STARTING_HANDLE_FIELD + LENGTH_ENDING_HANDLE_FIELD + LENGTH_ATTRIBUTE_TYPE_FIELD;
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
        byteBuffer.put(1, (byte) mAttPduLength);

        /// Put Att PDU
        // Att opcode
        byteBuffer.put(2, request_opcode);
        // starting handle
        byteBuffer.putShort(3, mStartingAttHandle);
        // ending handle
        byteBuffer.putShort(5, mEndingAttHandle);
        // attribute type
        byteBuffer.putShort(7, mAttributeTypeIn2);
    }

    @Override
    public void parseResponse(byte[] response) {
        super.parseResponse(response);
        if (response_opcode == AttributeOpcodeDefine.READ_BY_TYPE_RESPONSE) {
            byte handle_value_pair_length = response[1];
            int attribute_data_list_length = response.length - 2; // Attribute Opcode(1B) + Length(1B) + Attribute Data List(4 to (ATT_MTU- 2))
            byte[] attribute_data_list = new byte[attribute_data_list_length];
            System.arraycopy(response, 2, attribute_data_list, 0, attribute_data_list_length);

            if (getReadByTypeRequestCallback() != null) {
                getReadByTypeRequestCallback().onReadSuccess(handle_value_pair_length & 0x0FF, attribute_data_list);
            }
            mParseResult = AttributeParseResult.PARSE_SUCCESS;
        } else {
            if (getReadByTypeRequestCallback() != null) {
                getReadByTypeRequestCallback().onReceiveFailed(response_opcode, error_request_opcode, error_att_handle, error_code);
            }
        }
    }
}
