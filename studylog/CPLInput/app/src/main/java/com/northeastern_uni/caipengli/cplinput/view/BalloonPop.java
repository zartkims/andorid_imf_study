package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;


/**
 * Created by caipengli on 16年2月23日.
 * 这个就是弹框主逻辑
 */
public class BalloonPop extends PopupWindow {

    public static final int TIME_DELAY_SHOW = 0;
    public static final int TIME_DELAY_DISMISS = 200;

    private Context mContext;
    private Rect mPaddingRect = new Rect();

    private View mParent;
    private BalloonView mBalloonView;

    private int mMeasureSpecMode;
    private boolean mForceDismiss;

    private BalloonTimer mBalloonTimer;
    private int mParentLocationInWindow[] = new int[2];

    public BalloonPop(Context context, View parent, int measureSpecMode) {
        super(context);
        mContext = context;
        mParent = parent;
        mMeasureSpecMode = measureSpecMode;
        setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        setTouchable(false);
        setBackgroundDrawable(new ColorDrawable(0));

        mBalloonView = new BalloonView(context);
        mBalloonView.setClickable(false);
        setContentView(mBalloonView);
        mBalloonTimer = new BalloonTimer();
    }

    public void setBalloonBackground(Drawable drawable) {
        if (mBalloonView.getBackground() == drawable) return;
        mBalloonView.setBackground(drawable);
        if (null != drawable) {
            drawable.getPadding(mPaddingRect);
        } else {
            mPaddingRect.set(0, 0, 0, 0);
        }
    }

    /**
     *
     * @param text
     * @param fontSize
     * @param isBold
     * @param fontColor
     * @param width  这个是理想状态的宽度,实际的话还是会根据 pop 的内容决定
     * @param height 同理上理想的高度
     */
    public void setBalloonConfig(String text, float fontSize, boolean isBold,
                                 int fontColor, int width, int height) {
        mBalloonView.setTextConfig(text, fontSize, isBold, fontColor);
        setBalloonSize(width, height);
    }

    public void setBalloonConfig(Drawable icon, int width, int height) {
        mBalloonView.setIcon(icon);
        setBalloonSize(width, height);
    }

    //MeasureSpecMode 有三种状态 分别是 UNSPECIFIED(无限制) EXACTLY AT_MOST 所以你懂的
    private void setBalloonSize(int width, int height) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, mMeasureSpecMode);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, mMeasureSpecMode);

        mBalloonView.measure(widthMeasureSpec, heightMeasureSpec);
        int oldWidth = getWidth();
        int oldHeight = getHeight();
        int newWidth = mBalloonView.getMeasuredWidth() + getPaddingLeft() + getPaddingRight();
        int newHeight = mBalloonView.getMeasuredHeight() + getPaddingTop() + getPaddingBottom();
        setWidth(newWidth);
        setHeight(newHeight);

        mForceDismiss= false;
        if (isShowing()) {
            mForceDismiss = oldWidth != newWidth;
        }
    }

    public void delayedShow(long delay, int locationInParent[]) {
        if (mBalloonTimer.isPending()) {
            mBalloonTimer.removeTimer();//直接移除上一次
        }
        if (delay <= 0) { //立即
            mParent.getLocationInWindow(mParentLocationInWindow);
            showAtLocation(mParent, Gravity.LEFT | Gravity.TOP,
                    locationInParent[0], locationInParent[1]
                            + mParentLocationInWindow[1]);
        } else {
            mBalloonTimer.startTimer(delay, BalloonTimer.SHOW,
                    locationInParent, -1, -1);
        }
    }

    public void delayedUpdate(long delay, int locationInParent [], int width, int height) {
        mBalloonView.invalidate();
        if (mBalloonTimer.isPending()) {
            mBalloonTimer.removeTimer();
        }
        if (delay <= 0) {
            mParent.getLocationInWindow(mParentLocationInWindow);
            update(locationInParent[0], locationInParent[1] + mParentLocationInWindow[1], width, height);
        } else {
            mBalloonTimer.startTimer(delay, BalloonTimer.UPDATE, locationInParent, width, height);
        }
    }

    public void delayedDismiss(long delay) {
        if (mBalloonTimer.isPending()) {
            mBalloonTimer.removeTimer();
            if (0 != delay && BalloonTimer.HIDE != mBalloonTimer.getAction()) {
                mBalloonTimer.run();
            }
        }
        if (delay <= 0) {
            dismiss();
        } else {
            mBalloonTimer.startTimer(delay, BalloonTimer.HIDE, null, -1, -1);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public Rect getPadding() {
        return mPaddingRect;
    }

    public int getPaddingLeft() {
        return mPaddingRect.left;
    }

    public int getPaddingTop() {
        return mPaddingRect.top;
    }

    public int getPaddingRight() {
        return mPaddingRect.right;
    }

    public int getPaddingBottom() {
        return mPaddingRect.bottom;
    }

    public boolean needForceDismiss() {
        return mForceDismiss;
    }

    private class BalloonTimer extends Handler implements  Runnable {
        static final int SHOW = 1;
        static final int HIDE = 2;
        static final int UPDATE = 3;

        private  int mAction;

        private int mPositionInParent[] = new int [2];
        private int mWidth;
        private int mHeight;
        private boolean mTimerPending= false;

        public void startTimer(long time, int action, int positionInParent[], int width, int height) {
            mAction = action;
            if (HIDE != mAction) {
                mPositionInParent [0] = positionInParent[0];
                mPositionInParent [1] = positionInParent[1];
            }
            mWidth = width;
            mHeight = height;
            postDelayed(this, time);
            mTimerPending = true;//正在飞
        }

        public boolean removeTimer() {
            if (mTimerPending) {
                removeCallbacks(this);
                mTimerPending = false;
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            switch (mAction) {
                case SHOW:
                    mParent.getLocationInWindow(mParentLocationInWindow);
                    showAtLocation(mParent, Gravity.LEFT | Gravity.TOP,
                            mPositionInParent[0], mPositionInParent[1] + mParentLocationInWindow[1]);
                    break;
                case HIDE:
                    dismiss();
                    break;
                case UPDATE:
                    mParent.getLocationInWindow(mParentLocationInWindow);
                    update(mPositionInParent[0], mPositionInParent[1]
                            + mParentLocationInWindow[1], mWidth, mHeight);
                    break;
            }
            mTimerPending = false;
        }

        public boolean isPending() {
            return mTimerPending;
        }

        public int getAction() {
            return mAction;
        }
    }

    private class BalloonView extends View {
        private static final String OMIT = "..."; //太长了就省略

        private int mPaddingLeft = InputEnvironment.LEFT_PADDING;
        private int mPaddingRight = InputEnvironment.RIGHT_PADDING;
        private int mPaddingTop = InputEnvironment.TOP_PADDING;
        private int mPaddingBottom = InputEnvironment.BOTTOM_PADDING;
        private float mSuspensionPointWidth;//pop的宽度

        private Drawable mIcon;
        private String mLable;
        private int mLabelColor = Color.BLACK;
        private Paint mPaintLabel;
        private Paint.FontMetricsInt mFmi;


        public BalloonView(Context context) {
            super(context);
            mPaintLabel = new Paint();
            mPaintLabel.setColor(mLabelColor);
            mPaintLabel.setAntiAlias(true);
            mPaintLabel.setFakeBoldText(true);
            mFmi = mPaintLabel.getFontMetricsInt();
        }

        public void setTextConfig(String text, float fontSize, boolean isBold, int textColor) {
            mIcon = null;
            mLable = text;
            mPaintLabel.setTextSize(fontSize);
            mPaintLabel.setFakeBoldText(isBold);
            mPaintLabel.setColor(textColor);
            mFmi = mPaintLabel.getFontMetricsInt();
//            mSuspensionPointWidth = mPaintLabel.measureText(OMIT);
            mSuspensionPointWidth = mPaintLabel.measureText(text);
        }

        public void setIcon(Drawable icon) {
            mIcon = icon;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthMode == MeasureSpec.EXACTLY) { //确定的大小
                setMeasuredDimension(widthSize, heightSize);
                return;
            }
            int measuredWidth = mPaddingLeft + mPaddingRight;
            int measuredHeight = mPaddingTop + mPaddingBottom;

            if (null != mIcon) {
                measuredWidth += mIcon.getIntrinsicWidth();
                measuredHeight += mIcon.getIntrinsicHeight();
            } else if (null != mLable) {
                measuredWidth += mPaintLabel.measureText(mLable);
                measuredHeight += mFmi.bottom - mFmi.top; //直接就是字的高度了
            }
            if (widthSize > measuredWidth || widthMode == MeasureSpec.AT_MOST) {
                measuredWidth = widthSize;
            }

            if (heightSize > measuredHeight || heightMode == MeasureSpec.AT_MOST) {
                measuredHeight = heightSize;
            }
            int maxWidth = InputEnvironment.getInstance().getSkbWidth() -
                    mPaddingLeft - mPaddingRight;
            if (measuredWidth > maxWidth) {
                measuredWidth = maxWidth;
            }
            if (measuredWidth > maxWidth) {
                measuredWidth = maxWidth;
            }
            setMeasuredDimension(measuredWidth, measuredHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int width = getWidth();
            int height = getHeight();
            if (null != mIcon) {
                int marginLeft = (width - mIcon.getIntrinsicWidth()) / 2;
                int marginRight = width - mIcon.getIntrinsicWidth() - marginLeft;
                int marginTop = (height - mIcon.getIntrinsicHeight()) / 2;
                int marginBottom = height - mIcon.getIntrinsicHeight() - marginTop;
                mIcon.setBounds(marginLeft, marginTop, width - marginRight,
                        height - marginBottom);
                mIcon.draw(canvas);
            } else if (null != mLable) {
                float textWidth = mPaintLabel.measureText(mLable);
                float x = mPaddingLeft;
                x += (width - textWidth - mPaddingLeft -mPaddingRight) / 2;
                String lableToDraw = mLable;
                if (x < mPaddingLeft) {
                    x = mPaddingLeft;
                    lableToDraw = getLimitStringToDrawing(mLable, width - mPaddingLeft - mPaddingRight);
                }
                int fontHeight = mFmi.bottom - mFmi.top;
                float marginY = (height - fontHeight) / 2.0f;
                float y = marginY - mFmi.top;
                canvas.drawText(lableToDraw, x, y, mPaintLabel);  // 主要就是为了居中
            }
        }

        /**
         * 这个函数是点击候选词的时候就会触发
         * @param text
         * @param widthToDraw
         * @return
         */
        private String getLimitStringToDrawing(String text,float widthToDraw) {
            int strLen = text.length();
            if (strLen <= 1) return text;
            do { //循环截取至合适的长度
                strLen--;
                float width = mPaintLabel.measureText(text, 0, strLen);
                if (width + mSuspensionPointWidth <= widthToDraw || 1 >= strLen) {
                    return text.substring(0, strLen) + OMIT;
                }
            } while (true);
        }
    }

}


