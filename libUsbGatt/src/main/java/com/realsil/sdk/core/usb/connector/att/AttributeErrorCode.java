package com.realsil.sdk.core.usb.connector.att;

/**
 * The file define the code of The reason why the request has generated an error response.
 * <p>see: 《Bluetooth Core Specification V5.0 Vol3,PartF 3.4.1.1 Error Response》</p>
 *
 * @author xp.chen
 */
public interface AttributeErrorCode {

    /**
     * The attribute handle given was not valid on this server.
     */
    byte INVALID_HANDLE                   = 0x01;
    /**
     * The attribute cannot be read.
     */
    byte READ_NOT_PERMITTED               = 0x02;
    /**
     * The attribute cannot be written.
     */
    byte WRITE_NOT_PERMITTED              = 0x03;
    /**
     * The attribute PDU was invalid.
     */
    byte INVALID_PDU                      = 0x04;
    /**
     * The attribute requires authentication before it can be read or written.
     */
    byte INSUFFICIENT_AUTHENTICATION      = 0x05;
    /**
     * Attribute server does not support the request received from the client.
     */
    byte REQUEST_NOT_SUPPORTED            = 0x06;
    /**
     * Offset specified was past the end of the attribute.
     */
    byte INVALID_OFFSET                   = 0x07;
    /**
     * The attribute requires authorization before it can be read or written.
     */
    byte INSUFFICIENT_AUTHORIZATION       = 0x08;
    /**
     * Too many prepare writes have been queued.
     */
    byte PREPARE_QUEUE_FULL               = 0x09;
    /**
     * No attribute found within the given attribute handle range.
     */
    byte ATTRIBUTE_NOT_FOUND              = 0x0A;
    /**
     * The attribute cannot be read using the Read Blob Request.
     */
    byte ATTRIBUTE_NOT_LONG               = 0x0B;
    /**
     * The Encryption Key Size used for encrypting this link is insufficient.
     */
    byte INSUFFICIENT_ENCRYPTION_KEY_SIZE = 0x0C;
    /**
     * The attribute value length is invalid for the operation;
     */
    byte INVALID_ATTRIBUTE_VALUE_LENGTH   = 0x0D;
    /**
     * The attribute request that was requested has encountered an error that was unlikely, and therefore could
     * not be completed as requested.
     */
    byte UNLIKELY_ERROR                   = 0x0E;
    /**
     * The attribute requires encryption before it can be read or written.
     */
    byte INSUFFICIENT_ENCRYPTION          = 0x0F;
    /**
     * The attribute type is not a supported grouping attribute as defined by a higher layer specification.
     */
    byte UNSUPPORTED_GROUP_TYPE           = 0x10;
    /**
     * Insufficient Resources to complete the request.
     */
    byte INSUFFICIENT_RESOURCES           = 0x11;


}
