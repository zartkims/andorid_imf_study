package com.northeastern_uni.caipengli.cplinput.view;


import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ViewFlipper;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.logic.SoftKeyboard;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKey;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyboardPool;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.utils.SwitchKeyboardManager;
import com.northeastern_uni.caipengli.cplinput.R;

/**
 * Created by caipengli on 16年2月29日.
 */
public class SoftKeyboardContainer extends RelativeLayout implements View.OnTouchListener {

    /**允许的点击偏差*/
    private static final int Y_BIAS_CORRECTION = -10;
    /**做一些优化，忽略较小的移动提高效率*/
    private static final int MOVE_TOLERANCE = 6;
    private static final boolean POPUPWINDOW_FOR_PRESSED_UI = false;

    private int mSkbLayout = 0;
    private InputMethodService mService;
    private SwitchKeyboardManager mInputModeSwitcher;
    private GestureDetector mGestureDetector;
    private InputEnvironment mEnvironment;
    private ViewFlipper mSkbFlipper;
    /**pop提示*/
    private BalloonPop mBalloonPop;
    private BalloonPop mBalloonOnKey = null;
    /**主skb*/
    private SoftKeyboardView mMajorSkbView;

    private boolean mLastCandidatesShowing;
    private boolean mPopSkbShow = false;
    private boolean mPopupSkbNoResponse = false;
    private PopupWindow mPopupSkbWindow;
    /**pop的副skb 就是比如一开始不显示的　比如符号一开始是不显示的但是有*/
    private SoftKeyboardView mPopupSubSkbView;
    private int mPopupX;
    private int mPopupY;

    private volatile boolean mWaitForTouchUp = false;
    private volatile boolean mDiscardEvent = false;
    private int mYBaisCorrection = 0;
    private int mXLast;
    private int mYLast;
    private SoftKeyboardView mSkbv;
    private int mSkbVPosInContainer[] = new int [2];
    private SoftKey mSoftKeyDown = null;
    private SoftKeyboardContainer.LongPressTimer mLongPressTimer;
    private int mXyPosTmp[] = new int [2];

    public SoftKeyboardContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEnvironment = InputEnvironment.getInstance();
        mLongPressTimer = new LongPressTimer();
        mYBaisCorrection = Y_BIAS_CORRECTION;
        mBalloonPop = new BalloonPop(context, this, MeasureSpec.AT_MOST);
        if (POPUPWINDOW_FOR_PRESSED_UI) {
            mBalloonOnKey = new BalloonPop(context, this, MeasureSpec.AT_MOST);
        }
        mPopupSkbWindow = new PopupWindow(getContext());
        mPopupSkbWindow.setBackgroundDrawable(null);
        mPopupSkbWindow.setClippingEnabled(false);//设定不可超出window边界
        mGestureDetector = new GestureDetector(getContext(),new GestureDetector.SimpleOnGestureListener());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        InputEnvironment environment = InputEnvironment.getInstance();
        int measuredWidth = environment.getSkbWidth();
        int measuredHeight = environment.getSkbHeight() + getPaddingTop();
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth,
                MeasureSpec.EXACTLY);//父元素指定
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight,
                MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if (mSkbFlipper.isFlipping()) {
            resetKeyPress(0);
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        //忽略细小的手指移动(可能是因为手抖)以提高性能
        if (MotionEvent.ACTION_MOVE == event.getAction()) {
            if (Math.abs(x - mXLast) <= MOVE_TOLERANCE
                    && Math.abs(y - mYLast) <= MOVE_TOLERANCE) {
                return true;
            }
        }

        mXLast = x;
        mYLast = y;
        if (!mPopSkbShow) {//softkeyboard弹出了
            if (mGestureDetector.onTouchEvent(event)) {//mGestureDetector处理了
                resetKeyPress(0);
                mDiscardEvent = true;
                return true;
            }
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetKeyPress(0);
                mWaitForTouchUp = true;
                mDiscardEvent = false;
                mSoftKeyDown = null;
                mSkbv = null;
                mSkbv = inKeyboardView(x, y, mSkbVPosInContainer);
                if (null != mSkbv) {
//                    MYLOG.LOGI("going to map");
                    mSoftKeyDown = mSkbv.onKeyPress(x - mSkbVPosInContainer[0], y
                            - mSkbVPosInContainer[1], mLongPressTimer, false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (x < 0 || x >= getWidth() || y < 0 || y > getHeight()) {
                    break;
                }
                if (mDiscardEvent) {
                    resetKeyPress(0);
                    break;
                }
                if (mPopSkbShow && mPopupSkbNoResponse) {
                    break;
                }

                SoftKeyboardView softKeyboardView = inKeyboardView(x, y, mSkbVPosInContainer);
                if (null != softKeyboardView) {
                    if (mSkbv != softKeyboardView) {
                        mSkbv = softKeyboardView;
                        mSoftKeyDown = mSkbv.onKeyPress(x - mSkbVPosInContainer[0],
                                y - mSkbVPosInContainer[1], mLongPressTimer, true);
                    } else {
                        mSoftKeyDown = mSkbv.onKeyMove(x - mSkbVPosInContainer[0],
                                y - mSkbVPosInContainer[1]);
//                        Log.i("cpl","move key down : " + mSoftKeyDown.getKeyLabel() + "_" + mSoftKeyDown.getKeyCode());
                        if (null == mSoftKeyDown) {
//                            Log.i("cpl","move discard");
                            mDiscardEvent = true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDiscardEvent) {
                    resetKeyPress(0);
                    break;
                }
                mWaitForTouchUp = false;
                if (null != mSkbv) {
                    mSkbv.onKeyRelease(x - mSkbVPosInContainer[0], y - mSkbVPosInContainer[1]);
                }

                //当我们的pop没有占用这个事件的时候把这个事件 传递给我的输入法逻辑
                if (!mPopSkbShow || !mPopupSkbNoResponse) {
                    MYLOG.LOGI("action up 3");
                    responseKeyEvent(mSoftKeyDown);
                }

                if (mSkbv == mPopupSubSkbView && !mPopupSkbNoResponse) {
                    /**仅仅隐藏副键盘*/
                    dismissPopupSubSkb();
                }
                mPopupSkbNoResponse = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                resetKeyPress(0);
                break;
        }

        if (null == mSkbv) return false;
        return true;

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        MotionEvent newEvent = MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                event.getAction(), event.getX() + mPopupX, event.getY() + mPopupY,
                event.getPressure(), event.getSize(), event.getMetaState(), event.getXPrecision(),
                event.getYPrecision(), event.getDeviceId(), event.getEdgeFlags());
        boolean ret = onTouchEvent(newEvent);
        return ret;
    }

    private void responseKeyEvent(SoftKey key) {
        if (null == key) return;
        //// T ODO: 16年3月1日 mService.response.... finish now
        ((CPLIME)mService).responseSoftKeyEvent(key);
        return;
    }

    public void setService(InputMethodService service) {
        mService = service;
    }

    public void setSwitchManager(SwitchKeyboardManager manager) {
        this.mInputModeSwitcher = manager;
    }

    public boolean isCurrentSkbSticky() {
        if (null == mMajorSkbView) return true;
        if (null != mMajorSkbView.getSoftKeyboard()) {
            return mMajorSkbView.getSoftKeyboard().getStickyFlag();
        }
        return true;
    }

    public void toggleCandidateMode(boolean candidatesShowing) {
        if (null == mMajorSkbView || !mInputModeSwitcher.isChineseText()
                || mLastCandidatesShowing == candidatesShowing) {
            return;
        }

        SoftKeyboard skb = mMajorSkbView.getSoftKeyboard();
        if (null == skb) return;

        int state = mInputModeSwitcher.getTooggleStateForCnCand();
        if(!candidatesShowing) {
            skb.disableToggleState(state, false);
            skb.enableToggleStates(mInputModeSwitcher.getToggleStates());
        } else {
            skb.enableToggleState(state, false);
        }
        mMajorSkbView.invalidate();
    }

    public void updateInputMode() {
        int skbLayout = mInputModeSwitcher.getSkbLayout();
        if (mSkbLayout != skbLayout) {
            mSkbLayout = skbLayout;
            updateSkbLayout();
            MYLOG.LOGI("now is loaded the proper xml keyboard");
        }


        mLastCandidatesShowing = false;
        if (null == mMajorSkbView) return;
        SoftKeyboard skb = mMajorSkbView.getSoftKeyboard();
        if (null == skb) return;
        skb.enableToggleStates(mInputModeSwitcher.getToggleStates());
        invalidate();
    }

    private void updateSkbLayout() {
        int screenWidth = mEnvironment.getSkbWidth();
        int skbHeight = mEnvironment.getSkbHeight();
        if (null == mSkbFlipper) {
            mSkbFlipper = (ViewFlipper) this.findViewById(R.id.alpha_floatable);
        }
        mMajorSkbView = (SoftKeyboardView) mSkbFlipper.getChildAt(0);//强行这样啊
        SoftKeyboard majorSkb = null;
        SoftKeyboardPool skbPool = SoftKeyboardPool.getInstance();

        switch (mSkbLayout) {
            case R.xml.skb_qwerty:
                majorSkb = skbPool.getSoftKeyboard(getContext(), R.xml.skb_qwerty,
                         R.xml.skb_qwerty, screenWidth, skbHeight);
                break;
            case R.xml.skb_sym1:
                majorSkb = skbPool.getSoftKeyboard(getContext(), R.xml.skb_sym1,
                        R.xml.skb_sym1, screenWidth, skbHeight);
                break;
            case R.xml.skb_sym2:
                majorSkb = skbPool.getSoftKeyboard(getContext(), R.xml.skb_sym2,
                        R.xml.skb_sym2, screenWidth, skbHeight);
                break;
            case R.xml.skb_phone:
                majorSkb = skbPool.getSoftKeyboard(getContext(), R.xml.skb_phone,
                        R.xml.skb_phone, screenWidth, skbHeight);
                break;
            case R.xml.skb_smiley:
                majorSkb = skbPool.getSoftKeyboard(getContext(), R.xml.skb_smiley,
                        R.xml.skb_smiley, screenWidth, skbHeight);
                break;
            default:
        }
        if (null == majorSkb || !mMajorSkbView.setSoftKeyboard(majorSkb)) {
            MYLOG.LOGI("the major skb is null");
            return;
        }
        mMajorSkbView.setBalloonHint(mBalloonOnKey, mBalloonPop, false);
        mMajorSkbView.invalidate();
        mSkbv = mMajorSkbView;
    }

    /**隐藏副键盘*/
    private void dismissPopupSubSkb() {
        mPopupSkbWindow.dismiss();
        mPopSkbShow = false;
        dimMajorSoftKeyboard(false);
        resetKeyPress(0);
    }

    private void resetKeyPress(long delay) {
        mLongPressTimer.removeTimer();
        if (null != mSkbv) {
            mSkbv.resetKeyPress(delay);
        }
    }

    public boolean handleBack(boolean realAction) {
        if (mPopSkbShow) {
            if (!realAction) return true;
            dismissPopupSubSkb();
            mDiscardEvent = true;
            return true;
        }
        return false;
    }

    /**隐藏所有键盘*/
    public void dismissPopups() {
        handleBack(true);
        resetKeyPress(0);
    }

    /**如果能直接点到副键盘上就直接返回副键盘否则返回主键盘*/
    private SoftKeyboardView inKeyboardView(int x, int y, int positionInParent[]) {
        if (mPopSkbShow) {
            if (mPopupX <= x && mPopupX + mPopupSkbWindow.getWidth() > x
                    && mPopupY <= y && mPopupY + mPopupSkbWindow.getHeight() > y) {
                positionInParent[0] = mPopupX;
                positionInParent[1] = mPopupY;
                mPopupSubSkbView.setOffsetToSkbContainer(positionInParent);
                return mPopupSubSkbView;
            }
        }
        return mMajorSkbView;
    }

    private void popupSymbols() {
        int popupResId = mSoftKeyDown.getPopupResId();
        if (popupResId > 0) {
            int skbContainerWidth = getWidth();
            int skbContainerHeight = getHeight();
            int miniSkbWidth = (int) (skbContainerWidth * 0.8);
            int miniSkbHeight = (int) (skbContainerHeight * 0.23);

            SoftKeyboardPool skbPool = SoftKeyboardPool.getInstance();
            SoftKeyboard skb = skbPool.getSoftKeyboard(getContext(), popupResId,
                    popupResId, miniSkbWidth, miniSkbHeight);
            if (null == skb) return;
            mPopupX = (skbContainerWidth - skb.getSkbTotalWidth()) / 2;
            mPopupY = (skbContainerHeight - skb.getSkbTotalHeight()) / 2;

            if (null == mPopupSubSkbView) {
                mPopupSubSkbView = new SoftKeyboardView(getContext(), null);
                mPopupSubSkbView.onMeasure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            }
            mPopupSubSkbView.setOnTouchListener(this);
            mPopupSubSkbView.setSoftKeyboard(skb);
            mPopupSubSkbView.setBalloonHint(mBalloonOnKey, mBalloonPop, true);

            mPopupSkbWindow.setContentView(mPopupSubSkbView);//设定
            mPopupSkbWindow.setWidth(skb.getSkbCoreWidth()
                    + mPopupSubSkbView.getPaddingLeft()
                    + mPopupSubSkbView.getPaddingRight());
            mPopupSkbWindow.setHeight(skb.getSkbCoreHeight()
                    + mPopupSubSkbView.getPaddingTop()
                    + mPopupSubSkbView.getPaddingBottom());
            getLocationInWindow(mXyPosTmp);
            mPopupSkbWindow.showAtLocation(this, Gravity.NO_GRAVITY, mPopupX, mPopupY + mXyPosTmp[1]);
            mPopSkbShow = true;
            mPopupSkbNoResponse = true;
            dimMajorSoftKeyboard(true);
            resetKeyPress(0);
        }
    }

    private void dimMajorSoftKeyboard(boolean dim) {
        mMajorSkbView.dimSoftKeyboard(dim);
    }

    public SoftKeyboardView getSoftkeyboardView () {
        return mSkbv;
    }

    public class LongPressTimer extends Handler implements Runnable {
        /**长按触发本事件后 LONG_PRESS_TIMEOUT2 会调用*/
        public static final int LONG_PRESS_KEYNUM1 = 1;

        /**长按触发本事件后 LONG_PRESS_TIMEOUT3 会调用*/
        public static final int LONG_PRESS_KEYNUME2 = 3;
        public static final int LONG_PRESS_TIMEOUT1 = 500;
        public static final int LONG_PRESS_TIMEOUT2 = 100;
        public static final int LONG_PRESS_TIMEOUT3 = 100;

        private int mResponseTimes = 0;

        public void startTimer() {
            postAtTime(this, SystemClock.uptimeMillis() + LONG_PRESS_TIMEOUT1);
            mResponseTimes = 0;
        }

        public boolean removeTimer() {
            removeCallbacks(this);
            return true;
        }

        @Override
        public void run() {
            if (mWaitForTouchUp) {
                mResponseTimes++;
                if (mSoftKeyDown.repeatable()) { //这个按键可久按
                    if (mSoftKeyDown.isUserDefKey()) {
                        if (1 == mResponseTimes) {
                            if (mInputModeSwitcher.tryHandleLongPressSwitch(mSoftKeyDown.mKeyCode)) {//
                                mDiscardEvent = true;
                                resetKeyPress(0);
                            }
                        }
                    } else {
                        responseKeyEvent(mSoftKeyDown);
                        long timeout;
                        if (mResponseTimes < LONG_PRESS_KEYNUM1) {
                            timeout = LONG_PRESS_TIMEOUT1;
                        } else if (mResponseTimes < LONG_PRESS_KEYNUME2) {
                            timeout = LONG_PRESS_TIMEOUT2;
                        } else {
                            timeout = LONG_PRESS_TIMEOUT3;
                        }
                        //这个api灰常准确
                        postAtTime(this, SystemClock.uptimeMillis() + timeout);//现在之后的　ｔｉｍｅｏｕｔ
                    }
                } else {
                    if (1 == mResponseTimes) { //目前就切换输入法飞
                        popupSymbols();
                    }
                }
            }
        }
    }
}
