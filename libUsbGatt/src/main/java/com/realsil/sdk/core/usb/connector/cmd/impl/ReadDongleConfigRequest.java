package com.realsil.sdk.core.usb.connector.cmd.impl;

import com.realsil.sdk.core.usb.UsbGattCharacteristic;
import com.realsil.sdk.core.usb.connector.BaseRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdOpcodeDefine;
import com.realsil.sdk.core.usb.connector.cmd.UsbCmdParamLengthDefine;
import com.realsil.sdk.core.usb.connector.cmd.callback.QueryBTConnectStateRequestCallback;
import com.realsil.sdk.core.usb.connector.cmd.callback.ReadDongleConfigRequestCallback;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is used to read some USB dongle configuration information, such as all the information of OTA Characteristic.
 *
 * @author xp.chen
 */
public class ReadDongleConfigRequest extends BaseUsbRequest {


    /**
     * 2 - 16bit UUID;
     */
    private static final byte UUID_TYPE_VALUE_16_BIT  = 2;
    /**
     * 4 -32bit UUID
     */
    private static final byte UUID_TYPE_VALUE_32_BIT  = 4;
    /**
     * 16 - 128bit UUID
     */
    private static final byte UUID_TYPE_VALUE_128_BIT = 16;

    /**
     * Length of OTA Characteristic UUID Type = 1
     */
    private static final int LENGTH_CHARACTERISTIC_UUID_TYPE  = 1;
    /**
     * Length of OTA Characteristic ATT HANDLE = 2
     */
    private static final int LENGTH_CHARACTERISTIC_ATT_HANDLE = 2;

    private static final int LENGTH_CHARACTERISTIC_UUID_VALUE_2  = 2;
    private static final int LENGTH_CHARACTERISTIC_UUID_VALUE_4  = 4;
    private static final int LENGTH_CHARACTERISTIC_UUID_VALUE_16 = 16;

    /**
     * Add a callback interface to listen the usb dongle config information.
     *
     * @param readDongleConfigRequestCallback A callback is used to listen the usb dongle config information.
     */
    public void addReadDongleConfigRequestCallback(ReadDongleConfigRequestCallback readDongleConfigRequestCallback) {
        this.mBaseRequestCallback = (BaseRequestCallback) readDongleConfigRequestCallback;
    }

    /**
     * Get the callback currently used to listen for {@link ReadDongleConfigRequest}.
     *
     * @return A Callback currently for listening to {@link ReadDongleConfigRequest}.
     */
    public ReadDongleConfigRequestCallback getReadDongleConfigRequestCallback() {
        return (ReadDongleConfigRequestCallback) mBaseRequestCallback;
    }


    @Override
    public void setRequestOpcode() {
        this.request_opcode = UsbCmdOpcodeDefine.READ_USB_DONGLE_CONFIG_REQUEST;
    }

    @Override
    public void setMessageLength() {
        this.mSendMessageLength = UsbCmdParamLengthDefine.LENGTH_USB_CMD_OPCODE + UsbCmdParamLengthDefine.LENGTH_USB_CMD_NON_PARAM;
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

        /// Put USB PDU
        // Usb opcode
        byteBuffer.putShort(2, request_opcode);
    }

    @Override
    public void parseResponse(byte[] responseData) {
        super.parseResponse(responseData);
        if (response_opcode == request_opcode && status_code == STATUS_SUCCESS) {
            ByteBuffer buffer = ByteBuffer.wrap(responseData);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            int characteristic_num = responseData[22] & 0x0FF;
            List<UsbGattCharacteristic> list = new ArrayList<UsbGattCharacteristic>();

            int startIndex = 23;
            for (int i = 0; i < characteristic_num; i++) {
                byte uuid_type = buffer.get(startIndex);
                byte[] uuid_value = getUUIDValueLength(uuid_type);
                startIndex += LENGTH_CHARACTERISTIC_UUID_TYPE;
                System.arraycopy(responseData, startIndex, uuid_value, 0, uuid_value.length);
                startIndex += uuid_value.length;
                short att_handle = buffer.getShort(startIndex);
                startIndex += LENGTH_CHARACTERISTIC_ATT_HANDLE;
                UUID uuid = UUID.nameUUIDFromBytes(uuid_value);
                UsbGattCharacteristic usbGattCharacteristic = new UsbGattCharacteristic(uuid, att_handle, 0, 0);
                list.add(usbGattCharacteristic);
            }

            if (getReadDongleConfigRequestCallback() != null) {
                getReadDongleConfigRequestCallback().onReadOtaCharacteristicList(list);
            }
        } else {
            if (getReadDongleConfigRequestCallback() != null) {
                getReadDongleConfigRequestCallback().onReadFailed();
            }
        }
    }


    private static byte[] getUUIDValueLength(int uuidType) {
        byte[] uuid_value = null;
        switch (uuidType) {
            case UUID_TYPE_VALUE_16_BIT:
                uuid_value = new byte[UUID_TYPE_VALUE_16_BIT];
                break;
            case UUID_TYPE_VALUE_32_BIT:
                uuid_value = new byte[UUID_TYPE_VALUE_32_BIT];
                break;
            case UUID_TYPE_VALUE_128_BIT:
                uuid_value = new byte[UUID_TYPE_VALUE_128_BIT];
                break;
        }
        return uuid_value;
    }

}
