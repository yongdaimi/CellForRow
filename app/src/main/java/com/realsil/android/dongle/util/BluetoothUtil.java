package com.realsil.android.dongle.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;

public class BluetoothUtil {

    public static boolean hasBluetoothModule(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    public static int calculateSignalLevel(int numLevels, int rssi) {
        return WifiManager.calculateSignalLevel(rssi, numLevels);
    }


}
