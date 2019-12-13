package com.realsil.android.dongle.base;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;

import com.realsil.android.dongle.DongleApplication;
import com.realsil.android.dongle.R;


public abstract class BaseFragment extends Fragment implements OnClickListener, OnItemClickListener {

    protected DongleApplication mApplication;
    protected Context mContext;
    protected LayoutInflater mInflater;
    protected View contentView;
    private ViewGroup container;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    @SuppressWarnings("unchecked")
    protected <T extends View> T getView(int intResID) {
        return (T) contentView.findViewById(intResID);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    public String getLocalString(int resId) {
        return mContext.getResources().getString(resId);
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

    public void startActivity1(Intent intent) {
        ActivityOptionsCompat compat = ActivityOptionsCompat.makeCustomAnimation(getActivity(), android.R.transition.slide_left, android.R.transition.slide_right);
        ActivityCompat.startActivity(getActivity(),intent, compat.toBundle());

       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(new Intent(getContext(), TestActivity.class), ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
        } else {
            startActivity(new Intent(getContext(), TestActivity.class));
        }*/
    }
}