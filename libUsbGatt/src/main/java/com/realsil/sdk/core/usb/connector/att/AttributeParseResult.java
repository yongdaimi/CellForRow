package com.realsil.sdk.core.usb.connector.att;

/**
 * Status code returned when parsing attribute pdu
 * @author xp.chen
 */
public interface AttributeParseResult {

    /**
     * This status code is returned when the parsing is successful.
     */
    int PARSE_SUCCESS = 0;
    /**
     * This status code is returned when parsing fails.
     */
    int PARSE_FAILED = -1;

}
