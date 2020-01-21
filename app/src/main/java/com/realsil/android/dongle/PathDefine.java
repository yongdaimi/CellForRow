package com.realsil.android.dongle;

import android.os.Environment;

public interface PathDefine {

    String ROOT_PATH               = Environment.getExternalStorageDirectory().getAbsolutePath();
    String DONGLE_APP_PATH         = "Dongle";
    String DOWNLOAD_PATCH_LOG_PATH = "DownloadPatchLog";
    String CRASH_LOG_PATH          = "Crash";
    String MEDIA_PATH              = "Media";
    String FACTORY_SETTINGS_PATH   = "FactorySettings";
    String VIDEO_CACHE             = "VideoCache";
    String CACHE_TEMP              = "Temp";
    String MEDIA_EDITOR            = "Editor";

}
