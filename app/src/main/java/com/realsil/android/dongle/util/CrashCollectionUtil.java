package com.realsil.android.dongle.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.realsil.android.dongle.DongleApplication;
import com.realsil.android.dongle.PathDefine;


public class CrashCollectionUtil implements UncaughtExceptionHandler {

    private static final String TAG = "ERRORINFO";

    private static final String ANDROID_APP_VERSION_NAME = "versionName";
    private static final String ANDROID_APP_VERSION_CODE = "versionCode";
    private static final String ANDROID_SYSTEM_MODEL     = "MODEL";
    private static final String ANDROID_SYSTEM_SDK_INT   = "SDK_INT";
    private static final String ANDROID_SYSTEM_BRAND     = "BRAND";
    private static final String ANDROID_SYSTEM_PRODUCT   = "PRODUCT";
    private static final String ANDROID_SYSTEM_DISPLAY   = "DISPLAY";
    private static final String ANDROID_SYSTEM_HARDWARE  = "HARDWARE";


    private CrashCollectionUtil() {}

    private volatile static CrashCollectionUtil instance = null;

    public static CrashCollectionUtil getInstance() {
        if (instance == null) {
            synchronized (CrashCollectionUtil.class) {
                if (instance == null) {
                    instance = new CrashCollectionUtil();
                }
            }
        }
        return instance;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // Save current exception info
        saveErrorInfo2SD(ex);
        // Give tips to user
        showToast(DongleApplication.getInstance().getApplicationContext(), "...@_@...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Exit App
        DongleApplication.getInstance().exit();
    }


    /**
     * Save error info to SDCard
     *
     * @param ex
     */
    private String saveErrorInfo2SD(Throwable ex) {
        String fileName = null;
        StringBuffer sb = new StringBuffer();

        for (Map.Entry<String, String> entry : collectDeviceInfo(
                DongleApplication.getInstance().getApplicationContext()).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append(":").append(value).append("\n");
        }

        sb.append(collectExceptionInfo(ex));
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String savePath = PathDefine.ROOT_PATH + File.separator + PathDefine.REALSIL_APP_PATH + File.separator + PathDefine.CRASH_LOG_PATH + File.separator;
            LogX.i(TAG, "saveDir: " + savePath);
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            fileName = dir.toString() + File.separator + parserTime(
                    System.currentTimeMillis()) + ".log";
            try {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(sb.toString().getBytes());
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return fileName;
    }

    /**
     * Get current system info(the Android version, App version, Telephone version, Telephone argus
     */
    private HashMap<String, String> collectDeviceInfo(Context context) {
        HashMap<String, String> map = new HashMap<String, String>();
        PackageManager mPackageManager = context.getPackageManager();
        PackageInfo mPackageInfo = null;
        try {
            mPackageInfo = mPackageManager.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES
            );
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (mPackageInfo != null) {
            map.put(ANDROID_APP_VERSION_NAME, mPackageInfo.versionName);
            map.put(ANDROID_APP_VERSION_CODE, mPackageInfo.versionCode + "");
        }
        // Version
        map.put(ANDROID_SYSTEM_MODEL, Build.MODEL + ""); // Hardware info
        map.put(ANDROID_SYSTEM_SDK_INT, Build.VERSION.SDK_INT + ""); // Android version
        map.put(ANDROID_SYSTEM_BRAND, Build.BRAND); // Android relase name
        map.put(ANDROID_SYSTEM_PRODUCT, Build.PRODUCT); // Company
        map.put(ANDROID_SYSTEM_DISPLAY, Build.DISPLAY); // Telephone Screen argus
        map.put(ANDROID_SYSTEM_HARDWARE, Build.FINGERPRINT); // Hardware name
        return map;
    }

    private String collectExceptionInfo(Throwable throwable) {
        StringWriter mStringWriter = new StringWriter();
        PrintWriter mPrintWriter = new PrintWriter(mStringWriter);
        throwable.printStackTrace(mPrintWriter);
        mPrintWriter.close();
        Log.e(TAG, mStringWriter.toString());
        return mStringWriter.toString();
    }

    /**
     * Format current system time
     *
     * @param milliseconds current milliseconds
     * @return
     */
    private String parserTime(long milliseconds) {
        System.setProperty("user.timezone", "Asia/Shanghai");
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(tz);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        String times = format.format(new Date(milliseconds));
        return times;
    }

    private void showToast(final Context context, final String msg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }).start();
    }

}
