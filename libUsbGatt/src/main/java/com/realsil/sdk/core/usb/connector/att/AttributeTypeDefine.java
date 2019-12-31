package com.realsil.sdk.core.usb.connector.att;

/**
 * The file define the code of the attribute type.
 *
 * @author xp.chen
 */
public interface AttributeTypeDefine {

    /**
     * 0x2800 = Primary Service
     */
    short PRIMARY_SERVICE   = 0x2800;
    /**
     * 0x2801 = Secondary Service
     */
    short SECONDARY_SERVICE = 0x2801;
    /**
     * 0x2802 = Include Service
     */
    short INCLUDE_SERVICE   = 0x2802;
    /**
     * 0x2803 = Characteristic
     */
    short CHARACTERISTIC    = 0x2803;

}
