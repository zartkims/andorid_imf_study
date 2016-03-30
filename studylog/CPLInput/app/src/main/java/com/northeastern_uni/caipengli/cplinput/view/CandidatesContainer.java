package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.logic.CandidatesListener;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.R;

/**
 * Created by caipengli on 16年3月9日.
 * 这个是正常候选词(滚动)
 */
public class CandidatesContainer extends RelativeLayout implements
        View.OnTouchListener, ArrowUpdater {

    //// TODO: 16年3月9日 暂时使用透明度作为箭头之后会使用更好的图来设置
    private static int ARROW_ALPHA_ENABLED = 0xff;
    private static int ARROW_ALPHA_DISABLED = 0x40;


    private CandidatesListener mCvListener;

    private ImageButton mLeftArrowBtn;
    private ImageButton mRightArrowBtn;


    private CandidateScrollView mCandidateView;
    private CPLIME.DecodingInfo mDecInfo;

    private int xOffsetForFlipper;

    public CandidatesContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(CandidatesListener cvListener) {
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

    public void showCandidates(CPLIME.DecodingInfo decInfo) {
        if (null == decInfo) return;
        mDecInfo = decInfo;
        if (decInfo.isCandidatesListEmpty()) {
            showArrow(mLeftArrowBtn, false);
            showArrow(mRightArrowBtn, false);
        } else {
            showArrow(mLeftArrowBtn, true);
            showArrow(mRightArrowBtn, true);
        }
        mCandidateView.setDecodingInfo(mDecInfo);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        InputEnvironment env = InputEnvironment.getInstance();
        int measuredWidth = env.getSkbWidth();
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

    //// TODO: 16年3月9日  the future do it
    public boolean activeCurseBackward() {
        return  true;
    }

    public boolean activeCurseForward() {
        return true;
    }

    public boolean pageBackward() {
        if (null == mDecInfo) return false;
        mCandidateView.scrollBackWard();
        return true;
    }

    public boolean pageForward() {
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
        if (null == mCandidateView) return;
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

        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event.offsetLocation(-xOffsetForFlipper, 0);
        mCandidateView.onTouchEventReal(event);
        return true;
    }


}

interface ArrowUpdater {
    /**
     * @param left　能否左滑
     * @param right　能否右滑
     */
    void updateArrowStatus(boolean left, boolean right);
}