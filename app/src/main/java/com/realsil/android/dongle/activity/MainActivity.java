package com.realsil.android.dongle.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.realsil.android.dongle.R;
import com.realsil.android.dongle.base.BaseActivity;
import com.realsil.android.dongle.fragment.UsbAudioFragment;
import com.realsil.android.dongle.fragment.UsbDebugFragment;
import com.realsil.android.dongle.fragment.UsbDownloadPatchFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xp.chen
 */
public class MainActivity extends BaseActivity {


    private ViewPager2 vp_pager_2;

    private BottomNavigationView nv_bottom_tab_bar;

    private List<Fragment> mFragmentList;

    private AlertDialog mExitDialog;

    @Override
    protected void setContainer() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void init() {
        vp_pager_2 = findViewById(R.id.vp_pager);
        nv_bottom_tab_bar = findViewById(R.id.nv_bottom_tab_bar);
        nv_bottom_tab_bar.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
    }

    @Override
    protected void setListener() {
        vp_pager_2.registerOnPageChangeCallback(mOnPageChangeCallback);
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
        UsbAudioFragment usbAudioFragment = UsbAudioFragment.newInstance();
        mFragmentList.add(usbDownloadPatchFragment);
        mFragmentList.add(usbDebugFragment);
        mFragmentList.add(usbAudioFragment);

        vp_pager_2.setAdapter(mFragmentPagerAdapter);
    }



    private FragmentStateAdapter mFragmentPagerAdapter = new FragmentStateAdapter(this) {

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getItemCount() {
            return mFragmentList.size();
        }

    };

    private ViewPager2.OnPageChangeCallback mOnPageChangeCallback = new ViewPager2.OnPageChangeCallback() {

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
            vp_pager_2.setCurrentItem(menuItem.getOrder());
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
