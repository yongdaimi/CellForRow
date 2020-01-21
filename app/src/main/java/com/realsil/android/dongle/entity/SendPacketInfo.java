package com.realsil.android.dongle.entity;

/**
 * @author xp.chen
 */
public class SendPacketInfo {

    private boolean isLastPacket;
    private byte[] mSendPacketBuff;
    private byte mSendIndex;

    public SendPacketInfo(boolean isLastPacket, byte[] sendPacketBuff) {
        this.isLastPacket = isLastPacket;
        mSendPacketBuff = sendPacketBuff;
    }

    public boolean isLastPacket() {
        return isLastPacket;
    }

    public byte[] getSendPacketBuff() {
        return mSendPacketBuff;
    }

    public byte getSendIndex() {
        return mSendIndex;
    }

    public void setSendIndex(byte sendIndex) {
        mSendIndex = sendIndex;
    }
}
