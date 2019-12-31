package com.realsil.sdk.core.usb.connector;

/**
 * This class contains some constant definitions of usb dongle.
 *
 * @author xp.chen
 */
public final class UsbConfig {

    /**
     * Report id field of send message, It stands for Transparent Transport 1(Support Max Packet Size: 63).
     */
    public static final byte REPORT_ID_16 = 16;

    /**
     * Report id field of send message, It stands for Transparent Transport 2(Support Max Packet Size: 127).
     */
    public static final byte REPORT_ID_17 = 17;

    /**
     * Report id field of send message, It stands for Transparent Transport 3(Support Max Packet Size: 191).
     */
    public static final byte REPORT_ID_18 = 18;

    /**
     * Report id field of send message, It stands for Transparent Transport 4(Support Max Packet Size: 255).
     */
    public static final byte REPORT_ID_19 = 19;

    /**
     * Unknown Report ID.
     */
    public static final byte REPORT_ID_UNKNOWN = -1;

    /**
     * The maximum packet size supported by the Transparent Transport 1
     */
    private static final int MAX_PACKET_SIZE_63 = 63;

    /**
     * The maximum packet size supported by the Transparent Transport 2
     */
    private static final int MAX_PACKET_SIZE_127 = 127;

    /**
     * The maximum packet size supported by the Transparent Transport 3
     */
    private static final int MAX_PACKET_SIZE_191 = 191;

    /**
     * The maximum packet size supported by the Transparent Transport 4
     */
    private static final int MAX_PACKET_SIZE_255 = 255;

    /**
     * Select the appropriate Report ID based on the size of the packets sent.
     */
    public static byte selectComfortableReportID(int packageSize) {
        if (packageSize > MAX_PACKET_SIZE_255) {
            return REPORT_ID_UNKNOWN;
        } else if (packageSize > MAX_PACKET_SIZE_191) {
            return REPORT_ID_19;
        } else if (packageSize > MAX_PACKET_SIZE_127) {
            return REPORT_ID_18;
        } else if (packageSize > MAX_PACKET_SIZE_63) {
            return REPORT_ID_17;
        } else {
            return REPORT_ID_16;
        }
    }


}
