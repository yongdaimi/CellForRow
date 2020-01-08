package com.realsil.android.dongle.base;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.realsil.android.dongle.DongleApplication;
import com.realsil.android.dongle.R;


public abstract class BaseFragment extends Fragment {

    protected DongleApplication mApplication;

    protected Context mContext;

    protected LayoutInflater mInflater;

    protected View contentView;

    private ViewGroup container;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mApplication = DongleApplication.getInstance();
        this.mContext = mApplication.getContext();
        this.mInflater = inflater;
        this.container = container;
        setContainer();
        validateContainer();
        init();
        setListener();
        return contentView;
    }

    protected void setContentView(int layoutResID) {
        this.contentView = mInflater.inflate(layoutResID, container, false);
    }

    protected void setContentView(View view) {
        this.contentView = view;
    }

    /**
     * Init Fragment Content View
     */
    protected abstract void setContainer();

    /**
     * Init view
     */
    protected abstract void init();

    /**
     * Init UI listener
     */
    protected abstract void setListener();

    private void validateContainer() {
        if (contentView == null)
            throw new IllegalStateException("Please set the contentView at first!");
    }

    protected <T extends View> T findViewById(int resID) {
        return contentView.findViewById(resID);
    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    protected String getLocalString(int resId) {
        return mContext.getResources().getString(resId);
    }

    /** Show Debug Tips */
    protected void showToast(final String debugInfo) {
        if (__AssertUI__()) {
            BaseActivity activity = (BaseActivity) getActivity();
            activity.showToast(debugInfo);
        }
    }

    /** Show Debug Tips */
    protected void showToast(final int resId) {
        if (__AssertUI__()) {
            BaseActivity activity = (BaseActivity) getActivity();
            activity.showToast(resId);
        }
    }



    /**
     * assert UI exists
     */
    protected final boolean __AssertUI__() {
        if (getActivity() != null && !getActivity().isFinishing()) {
            return true;
        }
        return false;
    }

    protected void pushAnimActivity(Intent intent) {
        if (__AssertUI__()) {
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
        }
    }



}