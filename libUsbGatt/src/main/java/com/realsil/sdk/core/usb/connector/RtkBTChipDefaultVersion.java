package com.realsil.sdk.core.usb.connector;

/**
 * This interface defines all default version information about the bluetooth chip
 *
 * @author xp.chen
 */
public interface RtkBTChipDefaultVersion {

    /**
     * The LMP Subversion of the chip
     */
    interface LMPSubversion {
        /**
         * Chip Mode: RTL8723A
         */
        short RTL8723A = 0x1200;
        /**
         * Chip Mode: RTL8723B
         */
        short RTL8723B = (short) 0x8723;
        /**
         * Chip Mode: RTL8761A
         */
        short RTL8761A = (short) 0x8761;
        /**
         * hip Mode: RTL8821A
         */
        short RTL8821A = (short) 0x8821;
    }

    /**
     * The HCI Version of the chip
     */
    interface HCIVersion {
        /**
         * Chip Mode: RTL8723A
         */
        short RTL8723A = 0x000B;
        /**
         * Chip Mode: RTL8723B
         */
        short RTL8723B = 0x000B;
        /**
         * Chip Mode: RTL8761A
         */
        short RTL8761A = 0x000A;
        /**
         * hip Mode: RTL8821A
         */
        short RTL8821A = 0x000A;
    }


}
