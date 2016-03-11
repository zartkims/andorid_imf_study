/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.pinyin;

import com.android.inputmethod.pinyin.PinyinIME.DecodingInfo;
import com.android.inputmethod.pinyin.constants.MYLOG;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

interface ArrowUpdater {
    /**
     * @param left　能否左滑
     * @param right　能否右滑
     */
    void updateArrowStatus(boolean left, boolean right);
}


/**
 * Container used to host the two candidate views. When user drags on candidate
 * view, animation is used to dismiss the current candidate view and show a new
 * one. These two candidate views and their parent are hosted by this container.
 * <p>
 * Besides the candidate views, there are two arrow views to show the page
 * forward/backward arrows.
 * </p>
 */
public class CandidatesContainer extends RelativeLayout implements
        OnTouchListener, ArrowUpdater {
    /**
     * Alpha value to show an enabled arrow.
     */
    private static int ARROW_ALPHA_ENABLED = 0xff;

    /**
     * Alpha value to show an disabled arrow.
     */
    private static int ARROW_ALPHA_DISABLED = 0x40;

    /**
     * Animation time to show a new candidate view and dismiss the old one.
     */
    private static int ANIMATION_TIME = 200;

    /**
     * Listener used to notify IME that user clicks a candidate, or navigate
     * between them.
     */
    private CandidateViewListener mCvListener;

    /**
     * The left arrow button used to show previous page.
     */
    private ImageButton mLeftArrowBtn;

    /**
     * The right arrow button used to show next page.
     */
    private ImageButton mRightArrowBtn;

    /**
     * Decoding result to show.
     */
    private DecodingInfo mDecInfo;

     private CandidateScrollView mCandidateView;
    /**
     * The x offset of the flipper in this container.
     */
    private int xOffsetForFlipper;

    /**
     * Current page number in display.
     */
    private int mCurrentPage = -1;

    public CandidatesContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(CandidateViewListener cvListener) {
        mCvListener = cvListener;

        mLeftArrowBtn = (ImageButton) findViewById(R.id.arrow_left_btn);
        mRightArrowBtn = (ImageButton) findViewById(R.id.arrow_right_btn);
        mLeftArrowBtn.setOnTouchListener(this);
        mRightArrowBtn.setOnTouchListener(this);

        mCandidateView = (CandidateScrollView) findViewById(R.id.my_scroll_candidate);

        invalidate();
        requestLayout();
        mCandidateView.initialize(this, mCvListener);
    }

    public void showCandidates(PinyinIME.DecodingInfo decInfo,
            boolean enableActiveHighlight) {
        if (null == decInfo) return;
        mDecInfo = decInfo;
        mCurrentPage = 0;

        if (decInfo.isCandidatesListEmpty()) {
            showArrow(mLeftArrowBtn, false);
            showArrow(mRightArrowBtn, false);
        } else {
            showArrow(mLeftArrowBtn, true);
            showArrow(mRightArrowBtn, true);
        }
        mCandidateView.setDecodingInfo(mDecInfo);
//        updateArrowStatus(false, false);
        invalidate();
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void enableActiveHighlight(boolean enableActiveHighlight) {
//        mCandidateView.enableActiveHighlight(enableActiveHighlight);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Environment env = Environment.getInstance();
        int measuredWidth = env.getScreenWidth();
        int measuredHeight = getPaddingTop();
        measuredHeight += env.getHeightForCandidates();
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth,
                MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight,
                MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (null != mLeftArrowBtn) {
            xOffsetForFlipper = mLeftArrowBtn.getMeasuredWidth();
        }
    }

    public boolean activeCurseBackward() {
//        cpl
//        if (mFlipper.isFlipping() || null == mDecInfo) {
//            return false;
//        }
//
//        CandidateView cv = (CandidateView) mFlipper.getCurrentView();
//
//        if (cv.activeCurseBackward()) {
//            cv.invalidate();
//            return true;
//        } else {
//            return pageBackward(true, true);
//        }
        return  true;
    }

    public boolean activeCurseForward() {
//        cpl
//        if (mFlipper.isFlipping() || null == mDecInfo) {
//            return false;
//        }
//
//        CandidateView cv = (CandidateView) mFlipper.getCurrentView();
//
//        if (cv.activeCursorForward()) {
//            cv.invalidate();
//            return true;
//        } else {
//            return pageForward(true, true);
//        }

        return true;
    }

    public boolean pageBackward(boolean animLeftRight,
            boolean enableActiveHighlight) {
        if (null == mDecInfo) return false;
        mCandidateView.scrollBackWard();
        return true;
    }

    public boolean pageForward(boolean animLeftRight,
            boolean enableActiveHighlight) {
        if (null == mDecInfo) return false;
        MYLOG.LOGI("forward");
        mCandidateView.scrollForward();
        return true;
    }

    public int getActiveCandiatePos() {
        if (null == mDecInfo) return -1;
        return mCandidateView.getGlobalIndex();
    }

    public void updateArrowStatus(boolean left, boolean right) {
        if (mCurrentPage < 0) return;
//        boolean forwardEnabled = true;//cpl//
//         mDecInfo.pageForwardable(mCurrentPage);
//        boolean backwardEnabled = true;//cpl//mDecInfo.pageBackwardable(mCurrentPage);
//        if (backwardEnabled) {
//            enableArrow(mLeftArrowBtn, true);
//        } else {
//            enableArrow(mLeftArrowBtn, false);
//        }
//        if (forwardEnabled) {
//            enableArrow(mRightArrowBtn, true);
//        } else {
//            enableArrow(mRightArrowBtn, false);
//        }
        enableArrow(mLeftArrowBtn, left);
        enableArrow(mRightArrowBtn, right);

    }

    private void enableArrow(ImageButton arrowBtn, boolean enabled) {
        arrowBtn.setEnabled(enabled);
        if (enabled)
            arrowBtn.setAlpha(ARROW_ALPHA_ENABLED);
        else
            arrowBtn.setAlpha(ARROW_ALPHA_DISABLED);
    }

    private void showArrow(ImageButton arrowBtn, boolean show) {
        if (show)
            arrowBtn.setVisibility(View.VISIBLE);
        else
            arrowBtn.setVisibility(View.INVISIBLE);
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (v == mLeftArrowBtn) {
                mCvListener.onToRightGesture();
            } else if (v == mRightArrowBtn) {
                mCvListener.onToLeftGesture();
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            cpl
//            CandidateView cv = (CandidateView) mFlipper.getCurrentView();
//            cv.enableActiveHighlight(true);
//              mCandidateView.enableActiveHighlight(true);

        }

        return false;
    }

    // The reason why we handle candiate view's touch events here is because
    // that the view under the focused view may get touch events instead of the
    // focused one.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event.offsetLocation(-xOffsetForFlipper, 0);
        mCandidateView.onTouchEventReal(event);
        return true;
    }


}
