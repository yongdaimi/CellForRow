package com.realsil.android.dongle.entity;

import com.realsil.android.dongle.util.TimeUtil;

/**
 * @author xp.chen
 */
public class UsbMsg {

    public static final int MSG_TYPE_NORMAL = 0;
    public static final int MSG_TYPE_ERROR  = -1;

    private int mMsgType;
    private int mErrorCode;

    private String mMsgString;

    public UsbMsg(String msgText) {
        this(msgText, 0);
    }

    public UsbMsg(String msgText, int errorCode) {
        this.mErrorCode = errorCode;
        if (errorCode == 0) {
            this.mMsgType = MSG_TYPE_NORMAL;
            this.mMsgString = TimeUtil.getSimpleTimeStr() + "    " + msgText;
        } else {
            this.mMsgType = MSG_TYPE_ERROR;
            this.mMsgString = TimeUtil.getSimpleTimeStr() + "    " + msgText + ", err: "+errorCode;
        }
    }

    public int getMsgType() {
        return mMsgType;
    }

    public void setMsgType(int msgType) {
        mMsgType = msgType;
    }

    public String getMsgString() {
        return mMsgString;
    }

    public void setMsgString(String msgString) {
        mMsgString = msgString;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }
}
