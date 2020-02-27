package com.realsil.sdk.core.usb.connector;

/**
 * This interface defines all default version information (Lmp Subversion, HCI Revision)
 * about the bluetooth chip.
 *
 * @author xp.chen
 */
public interface RtkBTChipVersionInfo {

    /*
     ********************************
     *      LMP subversion Define
     ********************************
     */

    /**
     * Chip Type: RTL8723A
     */
    int LMP_SUB_VERSION_RTL8723A = 0x1200;
    /**
     * Chip Type: RTL8723B
     */
    int LMP_SUB_VERSION_RTL8723B = 0x8723;
    /**
     * Chip Type: RTL8761A
     */
    int LMP_SUB_VERSION_RTL8761A = 0x8761;
    /**
     * Chip Type: RTL8821A
     */
    int LMP_SUB_VERSION_RTL8821A = 0x8821;



    /*
     ********************************
     *      LMP subversion Define
     ********************************
     */

    /**
     * Chip Type: RTL8723A
     */
    int HCI_REVISION_RTL8723A = 0X000B;
    /**
     * Chip Type: RTL8723B
     */
    int HCI_REVISION_RTL8723B = 0X000B;
    /**
     * Chip Type: RTL8761A
     */
    int HCI_REVISION_RTL8761A = 0X000A;
    /**
     * Chip Type: RTL8761A
     */
    int HCI_REVISION_RTL8821A = 0X000A;

    /**
     * This two-dimensional array defines the default version information for each chip.
     * <p>Position of each chip version information: </p>
     * <ul>
     * <li>array[0] = RTL8723A</li>
     * <li>array[1] = RTL8723B</li>
     * <li>array[2] = RTL8761A</li>
     * <li>array[3] = RTL8821A</li>
     * </ul>
     */
    int CHIP_VERSION_INFO_TABLE[][] = {
            {LMP_SUB_VERSION_RTL8723A, HCI_REVISION_RTL8723A},
            {LMP_SUB_VERSION_RTL8723B, HCI_REVISION_RTL8723B},
            {LMP_SUB_VERSION_RTL8761A, HCI_REVISION_RTL8761A},
            {LMP_SUB_VERSION_RTL8821A, HCI_REVISION_RTL8821A}
    };

}
