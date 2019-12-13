package com.realsil.android.dongle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.realsil.android.dongle.util.CrashCollectionUtil;

import java.util.LinkedList;


public class DongleApplication extends Application {

    private LinkedList<Activity> mActivityList;

    private static DongleApplication instance;

    public static DongleApplication getInstance() {
        return instance;
    }

    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        instance = this;
        mActivityList = new LinkedList<>();
        initCrashCollectionUtil();
    }

    /*private void initIconCache()
    {
        long totalSize = Runtime.getRuntime().maxMemory() / 1024;
        int cacheSize = (int) (totalSize / 16);
        IconCache.getInstance().initialize(cacheSize);
    }*/

    /**
     * Init crash collection util
     */
    private void initCrashCollectionUtil() {
        CrashCollectionUtil.getInstance().init();
    }

    /**
     * Daemon service must be started when app process running ,otherwise will cause some problems.
     * For example,
     * sometimes RC action like "adb am start" command and will cause exceptions.
     */
    /*private void startDaemonService()
    {
        daemonTracker = new DaemonTracker(this);
        daemonTracker.startTrace();
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(new Intent(getApplicationContext(), Daemon.class));
        } else {
            startService(new Intent(getApplicationContext(), Daemon.class));
        }
    }*/
    public void addActivity(Activity activity) {
        mActivityList.add(activity);
    }

    /**
     * Just remove from list
     *
     * @param activity Activity to be remove from List
     */
    public void removeActivityFromList(Activity activity) {
        mActivityList.remove(activity);
    }

    public Context getContext() {
        return context;
    }

    public void exit() {
        try {
            for (Activity activity : mActivityList) {
                if (activity != null) {
                    activity.finish();
                }
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}