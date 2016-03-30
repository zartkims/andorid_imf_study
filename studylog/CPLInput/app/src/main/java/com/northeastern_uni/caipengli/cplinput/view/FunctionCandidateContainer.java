package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.R;
import com.northeastern_uni.caipengli.cplinput.logic.CandidatesListener;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;

/**
 * Created by caipengli on 16年3月17日.
 */
public class FunctionCandidateContainer extends LinearLayout implements View.OnClickListener {

//    private GestureDetector mGestureDetector;
    private MyLongPressTimer mLongPressTimer;
    private CPLIME.FloatWindowDragListener mDragListener;

    View mFloatBt;
    View mSettingBt;
    View mHideBt;
    View mReSizeBt;

    //如果被长按的话可进入拖动状态
    private boolean mIsLongPress = false;
    private Vibrator mVibrator;//长按的时候震动一下表示可以拖动
    protected long[] mVibratePattern = new long[] {1, 200};

    /**
     * 用于辅助拖动事件
     */
    private float mXf;
    private float mYf;

    /**
     * 能否拖动计算大小
     */
    private boolean mCanResize = false;

    private static float sIgnorX = -1;
    private static float sIgnorY = -1;

    private Rect [] mBtRect = new Rect[4];//第一个是设置　第二个是浮动　第三个是隐藏自己 第四个是重置大小
    private CandidatesListener mCandidateListener;

    public FunctionCandidateContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLongPressTimer = new MyLongPressTimer();
        mBtRect[0] = new Rect();
        mBtRect[1] = new Rect();
        mBtRect[2] = new Rect();
        mBtRect[3] = new Rect();
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
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mFloatBt = findViewById(R.id.bt_float_model);
        mSettingBt = findViewById(R.id.bt_setting);
        mHideBt = findViewById(R.id.bt_hide_self);
        mReSizeBt = findViewById(R.id.bt_resize);
        //用于飞float模式下
        mFloatBt.setOnClickListener(this);
        mSettingBt.setOnClickListener(this);
        mHideBt.setOnClickListener(this);
        mReSizeBt.setOnClickListener(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return onTouchEvent(ev);//自己先弄一波如果处理了就不给button
    }


    public boolean onTouchEvent(MotionEvent event) {
        if (!InputEnvironment.getInstance().isFloatMode()) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getRawX();
            float y = event.getRawY();
            float dx = x - mXf;
            float dy = y - mYf;
            if (sIgnorX < 0 || sIgnorY < 0) {
                sIgnorX = InputEnvironment.getInstance().getIngnorMoveDis(getContext());
                sIgnorY = InputEnvironment.getInstance().getIngnorMoveDis(getContext());
            }
            if (Math.abs(dx) < sIgnorX && Math.abs(dy) < sIgnorY) {//忽略细小的移动
                return true;
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mXf = event.getRawX();
                mYf = event.getRawY();
                mLongPressTimer.startTimer();
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mIsLongPress) { //如果之前没有触发长按的话
                    return false;
                }
                float x = event.getRawX();
                float y = event.getRawY();
                float dx = x - mXf;
                float dy = y - mYf;
                MYLOG.LOGI("to drag " + dx + "_" + dy);
                dragWindow(dx, dy);
                mXf = x;
                mYf = y;
                break;
            case MotionEvent.ACTION_UP:
                mLongPressTimer.removeTimer();
                MYLOG.LOGI("end move up");
                if (!mIsLongPress) { //正常的点击事件而已判断手指起来的地方如果是button就触发相应事件
                    calculateButtonRect();
                    float clickX = event.getRawX();
                    float clickY = event.getRawY();
                    if (mBtRect[0].left <= clickX && clickX < mBtRect[0].right
                            && mBtRect[0].top <= clickY && clickY < mBtRect[0].bottom) {
                        mCandidateListener.requestSetting();
                    } else if (mBtRect[1].left <= clickX && clickX < mBtRect[1].right
                            && mBtRect[1].top <= clickY && clickY < mBtRect[1].bottom) {//点击了浮动切换
                        mCandidateListener.onSwitchFloatMode();
                    } else if (mBtRect[2].left <= clickX && clickX < mBtRect[2].right
                            && mBtRect[2].top <= clickY && clickY < mBtRect[2].bottom) {
                        mCandidateListener.hideWindow();
                    } else if (mBtRect[3].left <= clickX && clickX < mBtRect[3].right
                            && mBtRect[3].top <= clickY && clickY < mBtRect[3].bottom) {
                        mCandidateListener.showResizePopup();
                    }
                } else {//说明是拖动后的actionUp
                    InputEnvironment.getInstance().saveFloatLocation(getContext());//这里做保存
                }
                mIsLongPress = false;//结束拖动
                break;

            case MotionEvent.ACTION_CANCEL:
                mLongPressTimer.removeTimer();
                break;
        }

        return true;
    }

    /**
     * 这个方法将会回调外部的窗口进行拖动
     * @param dx
     * @param dy
     */
    private void dragWindow(float dx, float dy) {
        InputEnvironment environment = InputEnvironment.getInstance();
        float locX = environment.getFloatingModeLocationX(getContext());
        float locY = environment.getFloatingModeLocationY(getContext());
        locX += dx;
        locY += dy;
//        if (locX < 0) locX = 0;
//        if (locY < 0) locY = 0;//不能出边界
        environment.setFloatingModeLocation((int) locX, (int) locY, getContext());
        if (null != mDragListener) {
            mDragListener.dragFloatWindow(dx, dy);
        }
    }

    private void calculateButtonRect() {
        int locaXY[] = new int[2];
        mSettingBt.getLocationOnScreen(locaXY);
        mBtRect[0] = new Rect(locaXY[0], locaXY[1],
                locaXY[0] + mHideBt.getWidth(), locaXY[1] + mHideBt.getHeight());

        mFloatBt.getLocationOnScreen(locaXY);
        mBtRect[1] = new Rect(locaXY[0], locaXY[1],
                locaXY[0] + mHideBt.getWidth(), locaXY[1] + mHideBt.getHeight());

        mHideBt.getLocationOnScreen(locaXY);
        mBtRect[2] = new Rect(locaXY[0], locaXY[1],
                locaXY[0] + mHideBt.getWidth(), locaXY[1] + mHideBt.getHeight());

        if (mCanResize) { //可以重置大小
//            MYLOG.LOGI("top_left_right_bottom " + locaXY[0] + "_" + locaXY[1]
//                    + "_" + (locaXY[0] + mReSizeBt.getWidth()) + "_" + (locaXY[1] + mReSizeBt.getHeight()));
            mReSizeBt.getLocationOnScreen(locaXY);
            mBtRect[3] = new Rect(locaXY[0], locaXY[1],
                    locaXY[0] + mReSizeBt.getWidth(), locaXY[1] + mReSizeBt.getHeight());
        }
    }

    public void setDragListener(CPLIME.FloatWindowDragListener listener) {
        this.mDragListener = listener;
    }

    private void tryVibrate() {
        if (mVibrator == null) {
            mVibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(mVibratePattern, -1);
    }

    public void setNotifyContainer(CandidatesListener candidatesListener) {
        this.mCandidateListener = candidatesListener;
    }

    /**
     * 这个是非floatmode下的事件处理
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_float_model) {
            mCandidateListener.onSwitchFloatMode();
        } else if (v.getId() == R.id.bt_setting) {
            mCandidateListener.requestSetting();
        } else if (v.getId() == R.id.bt_hide_self) {
            mCandidateListener.hideWindow();
        }
    }

    public boolean isLongPress() {
        return mIsLongPress;
    }

    private class MyLongPressTimer extends Handler implements Runnable {
        public static final int LONG_PRESS_TIMEOUT1 = 1000;
        public void startTimer() {
            postAtTime(this, SystemClock.uptimeMillis() + LONG_PRESS_TIMEOUT1);
        }

        public boolean removeTimer() {
            removeCallbacks(this);
            return true;
        }
        @Override
        public void run() {
            if (!mIsLongPress) {
                mIsLongPress = true;
                tryVibrate();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mLongPressTimer != null) {
            mLongPressTimer.removeTimer();
        }
    }

    public void setResizeable(boolean resizeable) {
        mCanResize = resizeable;
        if (resizeable) {
            mReSizeBt.setVisibility(View.VISIBLE);
        } else {
            mReSizeBt.setVisibility(View.GONE);
        }
    }
}
