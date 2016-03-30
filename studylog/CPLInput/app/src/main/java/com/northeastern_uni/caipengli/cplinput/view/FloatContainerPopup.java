package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.northeastern_uni.caipengli.cplinput.R;

/**
 * Created by caipengli on 16年3月21日.
 */
public class FloatContainerPopup extends PopupWindow {
//    /**
//     * 这个仅仅是用来占位的
//     */
    private LinearLayout mBlankKeyboard;
    public FloatContainerPopup(Context context) {
        super(context);
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mBlankKeyboard = (LinearLayout) mInflater.inflate(R.layout.blank_linear_layout, null);
        setContentView(mBlankKeyboard);
        setWidth(ViewGroup.LayoutParams.FILL_PARENT);
        setHeight(ViewGroup.LayoutParams.FILL_PARENT);
        setClippingEnabled(true);
    }
    public void removeMyView() {
        mBlankKeyboard.removeAllViews();
    }
    public void addMyView(View view) {
        mBlankKeyboard.addView(view);
    }
    public void setMinWidth(int minWidth) {
        mBlankKeyboard.setMinimumWidth(minWidth);
    }
    public void setMinHeight(int minHeight) {
        mBlankKeyboard.setMinimumHeight(minHeight);
    }
    public LinearLayout getMyRootLayout() {
        return mBlankKeyboard;
    }
}
