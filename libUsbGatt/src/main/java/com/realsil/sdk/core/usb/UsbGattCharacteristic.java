/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.realsil.sdk.core.usb;

import android.bluetooth.BluetoothGatt;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import java.util.UUID;

/**
 * Represents a Usb GATT Characteristic
 *
 * <p>A GATT characteristic is a basic data element used to construct a GATT service,
 * @author bingshanguxue
 */
public class UsbGattCharacteristic implements Parcelable {

    /**
     * Write characteristic, requesting acknoledgement by the remote device
     */
    public static final int WRITE_TYPE_DEFAULT = 0x02;

    /**
     * Write characteristic without requiring a response by the remote device
     */
    public static final int WRITE_TYPE_NO_RESPONSE = 0x01;

    /**
     * Write characteristic including authentication signature
     */
    public static final int WRITE_TYPE_SIGNED = 0x04;


    /**
     * The UUID of this characteristic.
     *
     */
    protected UUID mUuid;

    /**
     * Instance ID for this characteristic.
     *
     */
    protected int mInstance;


    /**
     * Write type for this characteristic.
     * See WRITE_TYPE_* constants.
     *
     */
    protected int mWriteType;


    /**
     * The cached value of this characteristic.
     *
     */
    protected byte[] mValue;


    /**
     * Create a new BluetoothGattCharacteristic.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param uuid The UUID for this characteristic
     * @param properties Properties of this characteristic
     * @param permissions Permissions for this characteristic
     */
    public UsbGattCharacteristic(UUID uuid, int properties, int permissions) {
        initCharacteristic( uuid, 0, properties, permissions);
    }


    /**
     * Create a new BluetoothGattCharacteristic
     *
     * @hide
     */
    public UsbGattCharacteristic(UUID uuid, int instanceId,
                                 int properties, int permissions) {
        initCharacteristic( uuid, instanceId, properties, permissions);
    }

    protected UsbGattCharacteristic(Parcel in) {
        mInstance = in.readInt();
        mWriteType = in.readInt();
        mValue = in.createByteArray();
    }

    public static final Creator<UsbGattCharacteristic> CREATOR = new Creator<UsbGattCharacteristic>() {
        @Override
        public UsbGattCharacteristic createFromParcel(Parcel in) {
            return new UsbGattCharacteristic(in);
        }

        @Override
        public UsbGattCharacteristic[] newArray(int size) {
            return new UsbGattCharacteristic[size];
        }
    };

    private void initCharacteristic(UUID uuid, int instanceId,
                                    int properties, int permissions) {
        mUuid = uuid;
        mInstance = instanceId;
        mValue = null;
        mWriteType = WRITE_TYPE_DEFAULT;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(new ParcelUuid(mUuid), 0);
        out.writeInt(mInstance);
        out.writeInt(mWriteType);
    }


    /**
     * Returns the UUID of this characteristic
     *
     * @return UUID of this characteristic
     */
    public UUID getUuid() {
        return mUuid;
    }

    /**
     * Returns the instance ID for this characteristic.
     *
     * <p>If a remote device offers multiple characteristics with the same UUID,
     * the instance ID is used to distuinguish between characteristics.
     *
     * @return Instance ID of this characteristic
     */
    public int getInstanceId() {
        return mInstance;
    }

    /**
     * Force the instance ID.
     *
     */
    public void setInstanceId(int instanceId) {
        mInstance = instanceId;
    }


    /**
     * Gets the write type for this characteristic.
     *
     * @return Write type for this characteristic
     */
    public int getWriteType() {
        return mWriteType;
    }

    /**
     * Set the write type for this characteristic
     *
     * <p>Setting the write type of a characteristic determines how the
     * {@link UsbGatt#writeCharacteristic} function write this
     * characteristic.
     *
     * @param writeType The write type to for this characteristic. Can be one of: {@link
     * #WRITE_TYPE_DEFAULT}, {@link #WRITE_TYPE_NO_RESPONSE} or {@link #WRITE_TYPE_SIGNED}.
     */
    public void setWriteType(int writeType) {
        mWriteType = writeType;
    }

    /**
     * Get the stored value for this characteristic.
     *
     * <p>This function returns the stored value for this characteristic as
     * retrieved by calling {@link UsbGatt#readCharacteristic}. The cached
     * value of the characteristic is updated as a result of a read characteristic
     * operation or if a characteristic update notification has been received.
     *
     * @return Cached value of the characteristic
     */
    public byte[] getValue() {
        return mValue;
    }

    /**
     * Updates the locally stored value of this characteristic.
     *
     * <p>This function modifies the locally stored cached value of this
     * characteristic. To send the value to the remote device, call
     * {@link UsbGatt#writeCharacteristic} to send the value to the
     * remote device.
     *
     * @param value New value for this characteristic
     * @return true if the locally stored value has been set, false if the requested value could not
     * be stored locally.
     */
    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }

    /**
     * Set the locally stored value of this characteristic.
     * <p>See {@link #setValue(byte[])} for details.
     *
     * @param value New value for this characteristic
     * @return true if the locally stored value has been set
     */
    public boolean setValue(String value) {
        mValue = value.getBytes();
        return true;
    }
}
