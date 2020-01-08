package com.realsil.android.dongle.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.realsil.android.dongle.R;
import com.realsil.android.dongle.base.BaseActivity;
import com.realsil.android.dongle.fragment.UsbDebugFragment;
import com.realsil.android.dongle.fragment.UsbDownloadPatchFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xp.chen
 */
public class MainActivity extends BaseActivity {


    private ViewPager vp_pager;

    private BottomNavigationView nv_bottom_tab_bar;

    private List<Fragment> mFragmentList;

    private AlertDialog mExitDialog;

    @Override
    protected void setContainer() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void init() {
        vp_pager = findViewById(R.id.vp_pager);
        nv_bottom_tab_bar = findViewById(R.id.nv_bottom_tab_bar);
        nv_bottom_tab_bar.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
    }

    @Override
    protected void setListener() {
        vp_pager.addOnPageChangeListener(mOnPageChangeListener);
        nv_bottom_tab_bar.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViewPager();
    }

    private void initViewPager() {
        mFragmentList = new ArrayList<Fragment>();
        UsbDownloadPatchFragment usbDownloadPatchFragment = UsbDownloadPatchFragment.newInstance();
        UsbDebugFragment usbDebugFragment = UsbDebugFragment.newInstance();
        mFragmentList.add(usbDownloadPatchFragment);
        mFragmentList.add(usbDebugFragment);
        vp_pager.setAdapter(mFragmentPagerAdapter);
    }


    private FragmentPagerAdapter mFragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager(),
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

    };


    private ViewPager.SimpleOnPageChangeListener mOnPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            nv_bottom_tab_bar.setSelectedItemId(position);
            nv_bottom_tab_bar.getMenu().getItem(position).setChecked(true);
        }

    };


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            vp_pager.setCurrentItem(menuItem.getOrder());
            return false;
        }
    };


    private void showExitTipsDialog() {
        if (mExitDialog == null) {
            mExitDialog = new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.app_tips_exit_application)
                    .setPositiveButton(R.string.app_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.app_cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dismissExitTipsDialog();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            mExitDialog = null;
                        }
                    })
                    .create();
            mExitDialog.show();
        }
    }

    private void dismissExitTipsDialog() {
        if (mExitDialog != null && mExitDialog.isShowing()) {
            mExitDialog.dismiss();
            mExitDialog = null;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitTipsDialog();
        }
        return super.onKeyDown(keyCode, event);
    }

}
