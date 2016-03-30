package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.R;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;

/**
 * Created by caipengli on 16年3月22日.
 */
public class ResizePop extends PopupWindow implements View.OnTouchListener {
    private static final int VALIDE_EDGE_DISTACNCE = 20;

    private View mRoot;
    private Context mContext;
    private CPLIME.RestSizeFinishListener mListener = null;

    private int mWidth;
    private int mHeight;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    private float mX;
    private float mY;

    private static float sTitleBarHeight = 0;
    private int mPreWidth;
    private int mPreHeight;
    private float mRateX;
    private float mRateY;

    private static float sValidEdgeDistance = -1;

    private boolean mTouchLeftEdge = false;
    private boolean mTouchRightEdge = false;
    private boolean mTouchTopEdge = false;
    private boolean mTouchBottomEdge = false;

    private float mMinWidth;
    private float mMinHeight;
    private float mMaxWidth;
    private float mMaxHeight;

    public ResizePop(Context context) {

        super(context);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layoutRoot = inflater.inflate(R.layout.resize_pop_layout, null);
        setContentView(layoutRoot);
        layoutRoot.setOnTouchListener(this);
        mRoot = layoutRoot.findViewById(R.id.layout_root_layout);
        mRoot.requestFocus();
        mRoot.setOnTouchListener(this);
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);

//        setBackgroundDrawable(null);
        setClippingEnabled(false);
        setOutsideTouchable(true);
        setTouchable(true);
        setFocusable(false);
        sValidEdgeDistance = VALIDE_EDGE_DISTACNCE * context.getResources().getDisplayMetrics().density;
        mMinWidth = InputEnvironment.getInstance().getMinFloatModeWidth(context);
        mMinHeight = InputEnvironment.getInstance().getMinFloatModeHeight(context);
        mMaxWidth = InputEnvironment.getInstance().getMaxFloatModeWidth(context);
        mMaxHeight = InputEnvironment.getInstance().getMaxFloatModeHeight(context);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (sValidEdgeDistance == -1) {
            sValidEdgeDistance = VALIDE_EDGE_DISTACNCE * mContext.getResources().getDisplayMetrics().densityDpi;
        }

        float dx = 0;
        float dy = 0;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                //由于取的是rawXY　而之前使用的是margin来设置的位置因此需要重新更新一下　否则会因为titlebar而出现偏差
                calculateTheOffset();

                mX = event.getRawX();
                mY = event.getRawY() - sTitleBarHeight;
                if (mX < mLeft - 10 || mRight + 10 < mX
                        || mY < mTop - 10 || mBottom + 10 < mY) { //点到外面了直接取消编辑，留些空隙给手指
                    dismiss();
                    return false;
                }
                mPreWidth = mWidth;
                mPreHeight = mHeight;
//                MYLOG.LOGI("my action down : " + mX + "_" + mY);
//                MYLOG.LOGI("mLeft_mRight : " + mLeft + "_" + mRight);
//                MYLOG.LOGI("mTop_mBottom : " + mTop + "_" + mBottom);
//                MYLOG.LOGI("sValidEdgeDistance : " + sValidEdgeDistance);
                mRateX = 1;
                mRateY = 1;
                if (mX - sValidEdgeDistance <= mLeft) mTouchLeftEdge = true;
                if (mY - sValidEdgeDistance <= mTop) mTouchTopEdge = true;
                if (mX + sValidEdgeDistance >= mRight ) mTouchRightEdge = true ;
                if (mY + sValidEdgeDistance >= mBottom) mTouchBottomEdge = true;
                break;
            case MotionEvent.ACTION_MOVE:

                if (!mTouchLeftEdge && !mTouchTopEdge && !mTouchRightEdge && !mTouchBottomEdge) {
                    return true;
                }
                float x,y;
                x = event.getRawX();
                y = event.getRawY() - sTitleBarHeight;
                dx = x - mX;
                dy = y - mY;
                //MYLOG.LOGI("my move : " + x + "_" + y);
                //现在还没添加边界判定
                if (mTouchLeftEdge) {
                    if (dx > 0) { //显然是在往右拉在变小
                        if (mWidth - dx <= mMinWidth) {//已经不能再小了
                            dx = 0;
                        } else {
                            mLeft += dx;
                        }
                    } else if (dy <= 0) { //往左边 为了易读不写else :)
                        if (mWidth - dx >= mMaxWidth) {//不能再大了
                            dx = 0;
                        } else {
                            mLeft += dx;
                        }
                    }
                } else if (mTouchRightEdge) { //左右边界只触发一边
                    if (dx > 0) {//在变大
                        if (mWidth + dx >= mMaxWidth) {//不能再大了
                            dx = 0;
                        } else {
                            mRight += dx;
                        }
                    } else if (dx <= 0) { //减小
                        if (mWidth + dx <= mMinWidth) {
                            dx = 0;
                        } else {
                            mRight += dx;
                        }
                    }
                }

                if (mTouchTopEdge) {
                    if (dy > 0) { //下拉
                        if (mHeight - dy <= mMinHeight) {
                            dy = 0;
                        } else {
                            mTop += dy;
                        }
                    } else if (dy <= 0) { //往上拉
                        if (mHeight - dy >= mMaxHeight) {
                            dy = 0;
                        } else {
                            mTop += dy;
                        }
                    }
                } else if (mTouchBottomEdge) {
                    if (dy > 0) { //下拉
                        if (mHeight + dy >= mMaxHeight) {
                            dy = 0;
                        } else {
                            mBottom += dy;
                        }
                    } else if (dy <= 0) { //往上拉
                        if (mHeight + dy <= mMinHeight) {
                            dy = 0;
                        } else {
                            mBottom += dy;
                        }
                    }
                }
                mX = x;
                mY = y;
                updateMyRootView(mLeft, mTop);
                break;
            case MotionEvent.ACTION_UP:
                mTouchLeftEdge = false;
                mTouchRightEdge = false;
                mTouchTopEdge = false;
                mTouchBottomEdge = false;
                mRateX = mWidth * 1.0f / mPreWidth * 1.0f;
                mRateY = mHeight* 1.0f / mPreHeight * 1.0f;
                float currentXRate = InputEnvironment.getInstance().getFloatModeXRatio();
                float currentYRate = InputEnvironment.getInstance().getFloatModeYRatio();
                currentXRate = currentXRate * mRateX;
                currentYRate = currentYRate * mRateY;
                InputEnvironment.getInstance().setFloatModeXRatio(currentXRate, mContext);
                InputEnvironment.getInstance().setFloatModeYRatio(currentYRate, mContext);
                //更新键盘
                InputEnvironment.getInstance().setFloatingModeLocation(mLeft, mTop, mContext);
                InputEnvironment.getInstance().saveFloatLocation(mContext);
                if (null != mListener) {
                    mListener.resetSizeFinish();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchLeftEdge = false;
                mTouchRightEdge = false;
                mTouchTopEdge = false;
                mTouchBottomEdge = false;
                MYLOG.LOGI("resize cancel");
                break;
        }
        return true;
    }

    private void calculateTheOffset() {
        int temp [] = new int [2];
        mRoot.getLocationOnScreen(temp);
        sTitleBarHeight = temp[1] - mTop;
    }

    private void updateMyRootView(int left, int top) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRoot.getLayoutParams();
        layoutParams.topMargin = top;
        layoutParams.leftMargin = left;
        layoutParams.width = mRight - mLeft;
        layoutParams.height = mBottom - mTop;
        mWidth = mRight - mLeft;
        mHeight = mBottom - mTop;
        mRoot.setLayoutParams(layoutParams);
        mRoot.invalidate();
    }

//    int mMarginLeft = 0;
//    int mMarginTop = 0;

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
//        mMarginLeft = x;
//        mMarginTop = y;

        super.showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRoot.getLayoutParams();
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        layoutParams.width = mWidth;
        layoutParams.height = mHeight;
        mRoot.setLayoutParams(layoutParams);
        resetMyRect(x, y);
    }

    private void resetMyRect(int x, int y) {
        mLeft = x;
        mTop = y;
        mRight = mLeft + mWidth;
        mBottom = mTop + mHeight;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    public void setRootWidth(int width) {
        this.mWidth = width;
//        resetMyRect(mLeft, mTop);
    }

    public void setRootHeight(int height) {
        this.mHeight = height;
        resetMyRect(mLeft, mTop);
    }

    public CPLIME.RestSizeFinishListener getListener() {
        return mListener;
    }

    public void setListener(CPLIME.RestSizeFinishListener listener) {
        this.mListener = listener;
    }
}
