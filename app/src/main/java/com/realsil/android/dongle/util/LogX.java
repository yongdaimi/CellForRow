package com.realsil.android.dongle.util;

import android.util.Log;

import com.realsil.android.dongle.AppConfig;


/**
 * Print Log
 *
 * @author xp.chen
 * @version V1.0
 */
public final class LogX {

    public static void i(String tag, String message) {
        if (AppConfig.developerMode) {
            Log.i(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (AppConfig.developerMode) {
            Log.e(tag, message);
        }
    }

    public static void w(String tag, String message) {
        if (AppConfig.developerMode) {
            Log.w(tag, message);
        }
    }
}
