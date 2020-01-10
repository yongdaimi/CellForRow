package com.realsil.android.dongle.util;

import java.util.Locale;

public class ByteUtils {

    public static String convertByteArr2String(byte[] bArr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bArr.length; i++) {
            builder.append(String.format(Locale.getDefault(), "%02x", bArr[i]).toUpperCase()).append(" ");
        }
        return builder.toString();
    }

}
