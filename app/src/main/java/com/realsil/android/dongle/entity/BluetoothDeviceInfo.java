package com.realsil.android.dongle.entity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

public class BluetoothDeviceInfo {

    private ScanResult mScanResult;
    private BluetoothDevice mBluetoothDevice;


    private int mRssi;

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        mRssi = rssi;
    }

    public ScanResult getScanResult() {
        return mScanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        mScanResult = scanResult;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        mBluetoothDevice = bluetoothDevice;
    }


}
