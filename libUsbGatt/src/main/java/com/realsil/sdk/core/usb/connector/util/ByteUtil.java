package com.realsil.sdk.core.usb.connector.util;

import java.util.Locale;

/**
 * Printing related tools
 * @author xp.chen
 */
public final class ByteUtil {

    public static String convertHexString(byte[] bArr) {
        if (bArr == null || bArr.length == 0) return null;
        StringBuilder stringBuffer = new StringBuilder();
        for (byte b : bArr) {
            stringBuffer.append(String.format(Locale.getDefault(), "%02x", b).toUpperCase()).append(" ");
        }
        return stringBuffer.toString();
    }

}
