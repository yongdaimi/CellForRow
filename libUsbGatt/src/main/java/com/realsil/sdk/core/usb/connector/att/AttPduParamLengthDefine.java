package com.realsil.sdk.core.usb.connector.att;

/**
 * This file defines the specific length of each parameter in the att pdu.
 */
public interface AttPduParamLengthDefine {


    /**
     * The length of the ATT Opcode in ATT PDU (1 Octets).
     */
    int LENGTH_ATT_OPCODE               = 1;
    /**
     * The length of the ATT Handle in ATT PDU (2 Octets).
     */
    int LENGTH_ATT_HANDLE               = 2;
    /**
     * The length of the client receive MTU size (2 Octets).
     */
    int LENGTH_ATT_CLIENT_RX_MTU        = 2;
    /**
     * The length of first requested handle number
     */
    int LENGTH_ATT_STARTING_HANDLE      = 2;
    /**
     * The length of last requested handle number
     */
    int LENGTH_ATT_ENDING_HANDLE        = 2;
    /**
     * The length of Attribute type.
     */
    int LENGTH_ATT_ATTRIBUTE_TYPE       = 2;
    /**
     * The length of Attribute group type.
     */
    int LENGTH_ATT_ATTRIBUTE_GROUP_TYPE = 2;


}
