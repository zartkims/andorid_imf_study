package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.northeastern_uni.caipengli.cplinput.logic.SoftKeyboard;
import com.northeastern_uni.caipengli.cplinput.domain.SettingHolder;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKey;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyType;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.utils.SoundManager;

import java.util.List;

/**
 * Created by caipengli on 16年2月23日.
 */
public class SoftKeyboardView extends View {

    private int mPaddingLeft = InputEnvironment.LEFT_PADDING;
    private int mPaddingRight = InputEnvironment.RIGHT_PADDING;
    private int mPaddingTop = InputEnvironment.TOP_PADDING;
    private int mPaddingBottom = InputEnvironment.BOTTOM_PADDING;

    private SoftKeyboard mSoftKeyboard;

    private BalloonPop mBalloonPopup;
    private BalloonPop mBalloonOnkey;//如果为空的直接画到key上

    private SoundManager mSoundManager;
    private SoftKey mSoftKeyDown;//上次按下的键
    private boolean mKeyPressed = false;
    /**相对keyboardcontainer的位置*/
    private int mOffsetToSkbContainer[] = new int[2];
    private int mHintLocationToSkbContainer[] = new int[2];
    private int mNormalKeyTextSize;
    private int mFunctionKeyTextSize;

    private SoftKeyboardContainer.LongPressTimer mLongPressTimer;
    /**这个的话是说明这个是否为可重复事件*/
    private boolean mRepeatForLongPress = false;
    private boolean mMovingNeverHidePopupBalloon = false;
    private Vibrator mVibrator;//震动
    protected long[] mVibratePattern = new long[] {1, 200};
    /**这个是预留的以后可能作为缓存,减少重绘的地方 */
    private Rect mDirtyRect = new Rect();
    private Paint mPaint;
    private Paint.FontMetricsInt mFmi;
    private boolean mDimSkb;


    public SoftKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSoundManager = SoundManager.getInstance(getContext());
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mFmi = mPaint.getFontMetricsInt();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = 0;
        int measuredHeight = 0;
        if (null != mSoftKeyboard) {
            measuredWidth = mSoftKeyboard.getSkbCoreWidth();
            measuredHeight = mSoftKeyboard.getSkbCoreHeight();
            measuredWidth += mPaddingLeft + mPaddingRight;
            measuredHeight += mPaddingTop + mPaddingBottom;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        if (null == mSoftKeyboard) return;
        canvas.translate(mPaddingLeft, mPaddingTop);
        InputEnvironment environment = InputEnvironment.getInstance();
        mNormalKeyTextSize = environment.getKeyTextSize(false);
        mFunctionKeyTextSize = environment.getKeyTextSize(true);

        int rowNum = mSoftKeyboard.getRowNum();
        int keyXMargin = mSoftKeyboard.getKeyXMargin();
        int keyYMargin = mSoftKeyboard.getKeyYMargin();
        for (int i = 0; i < rowNum; i++) {
            SoftKeyboard.KeyRow row = mSoftKeyboard.getKeyRowForDisplay(i);
            if (row == null) continue;
            List<SoftKey> softKeyList = row.mSoftKeys;
            int keyNum = softKeyList.size();
            for (int j = 0; j < keyNum; j ++) {
                SoftKey key = softKeyList.get(j);
                if (SoftKeyType.NORMAL_KEY_TYPE_ID == key.mKeyType.mKeyTypeId) {
                    mPaint.setTextSize(mNormalKeyTextSize);
                } else {
                    mPaint.setTextSize(mFunctionKeyTextSize);
                }
                drawSoftKey(canvas, key, keyXMargin, keyYMargin);
            }
        }
        if (mDimSkb) {
            mPaint.setColor(0xa0000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        }
        mDirtyRect.setEmpty();
    }

    private void drawSoftKey(Canvas canvas, SoftKey key, int keyXMargin, int keyYMargin) {
//        MYLOG.LOGI("the soft key is : " + key.mLeft + " " + " " + key.mTop
//                + " " + key.mRight + " " + key.mBottom);
        Drawable bg;
        int textColor;
        if (mKeyPressed && key == mSoftKeyDown) {
            bg = key.getKeyHlBg();
            textColor = key.getColorHl();
        } else {
            bg = key.getKeyBg();
            textColor = key.getColor();
        }
//        Drawable d = new ColorDrawable(Color.BLUE);
//        d.setBounds(softKey.mLeft, softKey.mTop, softKey.mRight, softKey.mBottom);
//        d.draw(canvas);
        if (null != bg) {
            bg.setBounds(key.mLeft + keyXMargin, key.mTop + keyYMargin,
                    key.mRight - keyXMargin, key.mBottom - keyYMargin);
            bg.draw(canvas);
        }
        String keyLabel = key.getKeyLabel();
        Drawable keyIcon = key.getKeyIcon();
        if (null != keyIcon) {
            Drawable icon = keyIcon;
            int marginLeft = (key.width() - icon.getIntrinsicWidth()) / 2;
            int marginRight = marginLeft;
            int marginTop = (key.height() - icon.getIntrinsicHeight()) / 2;
            int marginBottom = marginTop;
            icon.setBounds(key.mLeft + marginLeft, key.mTop + marginTop,
                    key.mRight - marginRight, key.mBottom - marginBottom);
            icon.draw(canvas);
        } else if (null != keyLabel) {
            mPaint.setColor(textColor);
            float x = key.mLeft + (key.width() - mPaint.measureText(keyLabel)) / 2.0f;
            int fontHeight = mFmi.bottom - mFmi.top;
            float marginY = (key.height() - fontHeight ) / 2.0f;
            float y = key.mTop + marginY + keyYMargin * 2;
            canvas.drawText(keyLabel, x, y + 1, mPaint);
        }
//        int fontHeight = mFmi.bottom - mFmi.top;
//        float marginY = (softKey.height() - fontHeight) / 2.0f;
////            float y = softKey.mTop + marginY - mFmi.top + mFmi.bottom / 1.5f;
//        float y = softKey.mBottom - marginY;
//
//        mPaint.setColor(Color.BLUE);
//        canvas.drawLine(softKey.mLeft, softKey.mTop, softKey.mRight, softKey.mBottom, mPaint);
//        canvas.drawLine(softKey.mLeft, softKey.height() / 2, softKey.mRight, softKey.height() / 2, mPaint);
//        mPaint.setColor(Color.RED);
//        canvas.drawLine(softKey.mLeft, y, softKey.mRight, y, mPaint);
//        mPaint.setColor(Color.YELLOW);
//        canvas.drawLine(softKey.mLeft, y, softKey.mRight, y - fontHeight, mPaint);
//        mPaint.setColor(Color.GREEN);
//        canvas.drawLine(softKey.mLeft, softKey.mTop + mFmi.bottom, softKey.mRight, softKey.mTop + mFmi.top, mPaint);

    }

    public void showBalloon(BalloonPop balloon, int balloonLocationToSkb[], boolean movePress) {
        if (InputEnvironment.getInstance().isFloatMode()) return;//如果是浮动模式就不暂时这个popup了
        long delay = BalloonPop.TIME_DELAY_SHOW;
        if (movePress) delay = 0;
        if (balloon.needForceDismiss()) balloon.delayedDismiss(0);
        if (!balloon.isShowing()) {
            balloon.delayedShow(delay, balloonLocationToSkb);
        } else {
            balloon.delayedUpdate(delay, balloonLocationToSkb, balloon.getWidth(), balloon.getHeight());
        }
    }

    /**pop消失　刷新下界面*/
    public void resetKeyPress(long delay) {
        if (!mKeyPressed) return;
        mKeyPressed = false;
        if (null != mBalloonOnkey) {
            mBalloonOnkey.delayedDismiss(delay);
        } else {
            if (null != mSoftKeyDown) {
                if (mDirtyRect.isEmpty()) {
                    mDirtyRect.set(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                            mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
                }
                invalidate(mDirtyRect);
            } else {
                invalidate();
            }
        }
        mBalloonPopup.delayedDismiss(delay);
    }

    /***
     *
     * @param x
     * @param y
     * @param longPressTimer
     * @param movePress 如果为true说明之前手指已经按下,现在是重新移动到这个键上.如果为false说明手指是第一次点击keyboard
     * @return
     */
    public SoftKey onKeyPress(int x, int y, SoftKeyboardContainer.LongPressTimer longPressTimer,boolean movePress) {
        mKeyPressed = false;
        boolean moveWithinPreviousKey = false;
        if (movePress) {
            SoftKey newKey = mSoftKeyboard.mapToKey(x, y);
            if (newKey == mSoftKeyDown) {
                moveWithinPreviousKey = true;
            }
            mSoftKeyDown = newKey;
        } else {
            mSoftKeyDown = mSoftKeyboard.mapToKey(x, y);
        }
        if (moveWithinPreviousKey || null == mSoftKeyDown) return  mSoftKeyDown;
        mKeyPressed = true;
        if (!movePress) {
            tryPlayKeyDown();
            tryVibrate();//震动
        }
        mLongPressTimer = longPressTimer;
        if (!movePress) {
            if (mSoftKeyDown.getPopupResId() > 0 || mSoftKeyDown.repeatable()) {
                mLongPressTimer.startTimer();
            }
        } else {
            mLongPressTimer.removeTimer();
        }
        //
        int desired_width;
        int desired_height;
        float textSize;
        InputEnvironment environment = InputEnvironment.getInstance();

        if (null != mBalloonOnkey) { //在key上有highlight
            Drawable keyHlBg = mSoftKeyDown.getKeyHlBg();

            int keyXMargin = mSoftKeyboard.getKeyXMargin();
            int keyYMargin = mSoftKeyboard.getKeyYMargin();
            desired_width = mSoftKeyDown.width() - 2 * keyXMargin;
            desired_height = mSoftKeyDown.height() - 2 * keyYMargin;
            textSize = environment
                    .getKeyTextSize(SoftKeyType.NORMAL_KEY_TYPE_ID != mSoftKeyDown.mKeyType.mKeyTypeId);
            Drawable icon = mSoftKeyDown.getKeyIcon();
            if (null != icon) {
                mBalloonOnkey.setBalloonConfig(icon, desired_width, desired_height);
            } else {
                mBalloonOnkey.setBalloonConfig(mSoftKeyDown.getKeyLabel(), textSize,
                        true, mSoftKeyDown.getColorHl(), desired_width, desired_height);
            }
            mHintLocationToSkbContainer[0] = mPaddingLeft + mSoftKeyDown.mLeft
                    - (mBalloonOnkey.getWidth() - mSoftKeyDown.width()) / 2;
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0]; // 这个的瞬间定位的相对的地方

            mHintLocationToSkbContainer[1] = mPaddingTop + (mSoftKeyDown.mBottom - keyYMargin)
                    - mBalloonOnkey.getHeight();
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1];
            showBalloon(mBalloonOnkey, mHintLocationToSkbContainer, movePress);
        } else {
            //现在先直接刷新一切
            mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                    mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
            invalidate(mDirtyRect);
        }

        if (mSoftKeyDown.needBalloon()) {
            Drawable balloonBg = mSoftKeyboard.getBalloonBackground();
            mBalloonPopup.setBalloonBackground(balloonBg);
            desired_width = mSoftKeyDown.width() + environment.getKeyBalloonWidthPlus();
            desired_height = mSoftKeyDown.height() + environment.getKeyBalloonHeightPlus();
            textSize = environment
                    .getBalloonTextSize(SoftKeyType.NORMAL_KEY_TYPE_ID != mSoftKeyDown.mKeyType.mKeyTypeId);
            Drawable iconPopup = mSoftKeyDown.getKeyIconPopup();
            if (null != iconPopup) {
                mBalloonPopup.setBalloonConfig(iconPopup, desired_width,
                        desired_height);
            } else {
                mBalloonPopup.setBalloonConfig(mSoftKeyDown.getKeyLabel(),
                        textSize, mSoftKeyDown.needBalloon(), mSoftKeyDown
                                .getColorBalloon(), desired_width,
                        desired_height);
            }
            mHintLocationToSkbContainer[0] = mPaddingLeft + mSoftKeyDown.mLeft
                    + -(mBalloonPopup.getWidth() - mSoftKeyDown.width()) / 2;
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0];
            mHintLocationToSkbContainer[1] = mPaddingTop + mSoftKeyDown.mTop
                    - mBalloonPopup.getHeight();
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1];
            showBalloon(mBalloonPopup, mHintLocationToSkbContainer, movePress);
        } else {
            mBalloonPopup.delayedDismiss(0);
        }
        if (mRepeatForLongPress) longPressTimer.startTimer();
        return mSoftKeyDown;
    }

    /**
     *
     * @param x
     * @param y
     * @return 最后停在哪个键上
     */
    public SoftKey onKeyRelease(int x, int y) {
        mKeyPressed = false;
        if (null == mSoftKeyDown) {
            return null;
        }
        mLongPressTimer.removeTimer();
        if (null != mBalloonOnkey) {
            mBalloonOnkey.delayedDismiss(BalloonPop.TIME_DELAY_DISMISS);
        } else {
            mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                    mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
            invalidate(mDirtyRect);
        }

        if (mSoftKeyDown.needBalloon()) {
            mBalloonPopup.delayedDismiss(BalloonPop.TIME_DELAY_DISMISS);
        }

        if (mSoftKeyDown.moveWithinKey(x - mPaddingLeft, y - mPaddingTop)) {
            return  mSoftKeyDown;
        }
        return null;
    }

    /**
     *  滑动过程主要是一些popup的更新
     * @param x
     * @param y
     * @return
     */
    public SoftKey onKeyMove(int x, int y) {
        if (null == mSoftKeyDown) return null;
        if (mSoftKeyDown.moveWithinKey(x - mPaddingLeft, y - mPaddingTop)) return  mSoftKeyDown;
        // 这块区域脏了.
        mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                mSoftKeyDown.mRight, mSoftKeyDown.mBottom);

        if (mRepeatForLongPress) {
            if (mMovingNeverHidePopupBalloon) {
                return onKeyPress(x, y , mLongPressTimer, true);
            }
            if (null != mBalloonOnkey) {
                mBalloonOnkey.delayedDismiss(0);
            } else {
                invalidate(mDirtyRect);
            }
            if (mSoftKeyDown.needBalloon()) {
                mBalloonPopup.delayedDismiss(0);
            }
            if (null != mLongPressTimer) {
                mLongPressTimer.removeTimer();
            }
            return onKeyPress(x, y, mLongPressTimer, true);
        } else {
            return  onKeyPress(x, y, mLongPressTimer, true);
        }
    }

    private void tryPlayKeyDown() {
        if (SettingHolder.getKeySound()) {
            mSoundManager.playKeyDown();
        }
    }

    private void tryVibrate() {
        if (!SettingHolder.getVibrate()) {
            return;
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(mVibratePattern, -1);
    }

    public boolean setSoftKeyboard(SoftKeyboard softKeyboard) {
        if (null == softKeyboard) return false;
        mSoftKeyboard = softKeyboard;
        Drawable bg = mSoftKeyboard.getSkbBackground();
        if (null != bg) {
            setBackground(bg);
        }
        return true;
    }

    public SoftKeyboard getSoftKeyboard() {
        return mSoftKeyboard;
    }

    public void setBalloonHint(BalloonPop balloonOnKey,
                               BalloonPop balloonPopup, boolean movingNeverHidePopup) {
        mBalloonOnkey = balloonOnKey;
        mBalloonPopup = balloonPopup;
        mMovingNeverHidePopupBalloon = movingNeverHidePopup;
    }

    public void setOffsetToSkbContainer(int offsetToSkbContainer[]) {
        mOffsetToSkbContainer[0] = offsetToSkbContainer[0];
        mOffsetToSkbContainer[1] = offsetToSkbContainer[1];
    }

    public void dimSoftKeyboard(boolean dim) {
        mDimSkb = dim;
        invalidate();
    }
}
