package com.realsil.sdk.core.usb.connector.att;

/**
 * This interface define the attribute PDU operation code value.
 * <p>see: 《Bluetooth Core Specification V5.0 Vol3,PartF 3.4.8 Attribute Opcode Summary》</p>
 *
 * @author xp.chen
 */
public interface AttributeOpcodeDefine {

    /**
     * 0x01 = ERROR_RESPONSE
     */
    byte ERROR_RESPONSE              = 0x01;
    /**
     * 0x02 = Exchange MTU Request
     */
    byte EXCHANGE_MTU_REQUEST        = 0x02;
    /**
     * 0x03 = Exchange MTU Response
     */
    byte EXCHANGE_MTU_RESPONSE       = 0x03;
    /**
     * 0x04 = Find Information Request
     */
    byte FIND_INFORMATION_REQUEST    = 0x04;
    /**
     * 0x05 = Find Information Response
     */
    byte FIND_INFORMATION_RESPONSE   = 0x05;
    /**
     * 0x06 = Find By Type Value Request
     */
    byte FIND_BY_TYPE_VALUE_REQUEST  = 0x06;
    /**
     * 0x07 = Find By Type Value Response
     */
    byte FIND_BY_TYPE_VALUE_RESPONSE = 0x07;
    /**
     * 0x08 = Read By Type Request
     */
    byte READ_BY_TYPE_REQUEST        = 0x08;
    /**
     * 0x09 = Read By Type Response
     */
    byte READ_BY_TYPE_RESPONSE       = 0x09;
    /**
     * 0x0A = Read Request
     */
    byte READ_REQUEST                = 0x0A;
    /**
     * 0x0B = Read Response
     */
    byte READ_RESPONSE               = 0x0B;
    /**
     * 0X0C = Read Blob Request
     */
    byte READ_BLOB_REQUEST           = 0x0C;
    /**
     * 0x0D = Read Blob Response
     */
    byte READ_BLOB_RESPONSE          = 0x0D;
    /**
     * 0x0E = Read Multiple Request
     */
    byte READ_MULTIPLE_REQUEST       = 0x0E;
    /**
     * 0x0F = Read Multiple Response
     */
    byte READ_MULTIPLE_RESPONSE      = 0x0F;
    /**
     * 0x10 = Read by Group Type Request
     */
    byte READ_BY_GROUP_TYPE_REQUEST  = 0x10;
    /**
     * 0x11 = Read by Group Type Response
     */
    byte READ_BY_GROUP_TYPE_RESPONSE = 0x11;
    /**
     * 0x12 = Write Request
     */
    byte WRITE_REQUEST               = 0x12;
    /**
     * 0x13 = Write Response
     */
    byte WRITE_RESPONSE              = 0x13;
    /**
     * 0x52 = Write Command
     */
    byte WRITE_COMMAND               = 0x52;
    /**
     * 0x16 = Prepare Write Request
     */
    byte PREPARE_WRITE_REQUEST       = 0x16;
    /**
     * 0x17 = Prepare Write Response
     */
    byte PREPARE_WRITE_RESPONSE      = 0x17;
    /**
     * 0x18 = Execute Write Request
     */
    byte EXECUTE_WRITE_REQUEST       = 0x18;
    /**
     * 0x19 = Execute Write Response
     */
    byte EXECUTE_WRITE_RESPONSE      = 0x19;
    /**
     * 0x1B = Handle Value Notification
     */
    byte HANDLE_VALUE_NOTIFICATION   = 0x1B;
    /**
     * 0x1D = Handle Value Indication
     */
    byte HANDLE_VALUE_INDICATION     = 0x1D;
    /**
     * 0x1E = Handle Value Confirmation
     */
    byte HANDLE_VALUE_CONFIRMATION   = 0x1E;
    /**
     * 0xD2 = Signed Write Command
     */
    byte SIGNED_WRITE_COMMAND        = (byte) 0xD2;

}


