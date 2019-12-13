package com.realsil.android.dongle;

import android.os.Environment;

public interface PathDefine {

    String ROOT_PATH             = Environment.getExternalStorageDirectory().toString();
    String REALSIL_APP_PATH      = "Dongle";
    String REALSIL_UPGRADE_PATH  = "Upgrade";
    String CRASH_LOG_PATH        = "Crash";
    String MEDIA_PATH            = "Media";
    String FACTORY_SETTINGS_PATH = "FactorySettings";
    String VIDEO_CACHE           = "VideoCache";
    String CACHE_TEMP            = "Temp";
    String MEDIA_EDITOR          = "Editor";

}
