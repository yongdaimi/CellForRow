package com.realsil.android.dongle.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public class ToolbarX extends Toolbar {


    private static final int TITLE_TEXT_VIEW_SIZE = 16;

    public ToolbarX(Context context) {
        this(context, null);
    }

    public ToolbarX(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ToolbarX(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        makeTitleCenter(context);
    }


    private void makeTitleCenter(Context context) {

        // Center title and subtitle
        View titleView = getChildAt(0);
        View subTitleView = getChildAt(1);
        if (titleView instanceof TextView) {
            TextView titleTextView = (TextView) titleView;
            ViewGroup.LayoutParams titleViewLayoutParams = titleTextView.getLayoutParams();
            titleViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            // set text size
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_VIEW_SIZE);
            titleTextView.setBackgroundColor(Color.RED);
        }

        if (subTitleView instanceof TextView) {
            TextView subTitleTextView = (TextView) subTitleView;
            ViewGroup.LayoutParams titleViewLayoutParams = subTitleTextView.getLayoutParams();
            titleViewLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            subTitleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        }

    }


}
