package com.android.inputmethod.pinyin;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.android.inputmethod.pinyin.ArrowUpdater;
import com.android.inputmethod.pinyin.BalloonHint;
import com.android.inputmethod.pinyin.CandidateViewListener;
import com.android.inputmethod.pinyin.constants.Constants;
import com.android.inputmethod.pinyin.constants.MYLOG;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Created by caipengli on 16年3月8日.
 */
public class CandidateScrollView extends View implements Observer{
    /**
     * The minimum width to show a item.
     */
    private static final float MIN_ITEM_WIDTH = 20;
    /**
     * 预期的字体大小 dp
     */
    private static int sDesiredCandidatesSize = 22;
    private static int sMinCandidatesSize = 18;

    protected int mPaddingLeft = Constants.LEFT_PADDING;
    protected int mPaddingRight = Constants.RIGHT_PADDING;
    protected int mPaddingTop = Constants.TOP_PADDING;
    protected int mPaddingBottom = Constants.BOTTOM_PADDING;
    private int mContentWidth;
    private int mContentHeight;

    private ArrowUpdater mArrowUpdater;
    private CandidateViewListener mCvListener;

    private Drawable mActiveCellDrawable;
    private Drawable mSeparatorDrawable;
    private int mImeCandidateColor;
    private int mRecommendedCandidateColor;
    private int mImeCandidateTextSize;
    private int mRecommendedCandidateTextSize;
    private int mNormalCandidateColor = Color.BLACK;
    private int mActiveCandidateColor = Color.RED;//默认是红色

    /**当前*/
    private int mCurCandidateTextSize;
    private Paint mCandidatesPaint;
    private float mCandidateMargin;
    /***
     * 这个属暂时保留
     */
    private int mActiveCandInPage;


    private Paint.FontMetricsInt mFmiCandidates;
    private int mLocationTmp[] = new int[2];
    private RectF mActiveCellRect;
    private boolean mIsMoving = false;
    private float mOriX;
    private float mOriY;
    private boolean mIsSliding = false;

    private boolean isOverHalf = false;

    /**
     * 这个是表示总的x的偏移 用户滑动之后依然可以记录合适的x指向的Rect
     */
    private float mTotalMoveX = 0;
    /**
     * 选中的那个item的index
     * 这个关系到如果划过了一半的话就去加载下一页
     */
    private int mGlobalItemIndex = 0;
    private Vector<RectF> mCandidateRects = new Vector<RectF>();
    private PinyinIME.DecodingInfo mDecInfo;

    public CandidateScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources r = context.getResources();

        Configuration conf = r.getConfiguration();
        if (conf.keyboard == Configuration.KEYBOARD_NOKEYS
                || conf.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
//            mShowFootnote = false;
        }

        mActiveCellDrawable = r.getDrawable(R.drawable.bg_candidate_background);
        mSeparatorDrawable = r.getDrawable(R.drawable.candidates_vertical_line);
        mCandidateMargin = r.getDimension(R.dimen.candidate_margin_left_right);

        mImeCandidateColor = r.getColor(R.color.candidate_color);
        mRecommendedCandidateColor = r.getColor(R.color.recommended_candidate_color);
        mNormalCandidateColor = mImeCandidateColor;
        mActiveCandidateColor = r.getColor(R.color.active_candidate_color);

        mCandidatesPaint = new Paint();
        mCandidatesPaint.setAntiAlias(true);
        mActiveCellRect = new RectF();

    }

    public void initialize(ArrowUpdater arrowUpdater, /*BalloonHint balloonHint,*/
                            CandidateViewListener cvListener) {
        mArrowUpdater = arrowUpdater;
//        mBalloonHint = balloonHint;
        mCvListener = cvListener;
    }

    public void setDecodingInfo(PinyinIME.DecodingInfo decInfo) {
        if (null == decInfo) return;
        mDecInfo = decInfo;
        mDecInfo.addObserver(this);
//        mPageNoCalculated = -1;

        if (mDecInfo.candidatesFromApp()) {
            mNormalCandidateColor = mRecommendedCandidateColor;
            mCurCandidateTextSize = mRecommendedCandidateTextSize;
        } else {
            mNormalCandidateColor = mImeCandidateColor;
            mCurCandidateTextSize = mImeCandidateTextSize;
        }
        if (mCandidatesPaint.getTextSize() != mCurCandidateTextSize) {
            mCandidatesPaint.setTextSize(mCurCandidateTextSize);
            mFmiCandidates = mCandidatesPaint.getFontMetricsInt();
//            mCurCandidateTextSize = mCandidatesPaint.measureText(SUSPENSION_POINTS);
        }

        // Remove any pending timer for the previous list.
//        mTimer.removeTimer();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mOldWidth = getMeasuredWidth();
        int mOldHeight = getMeasuredHeight();

        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(),
                widthMeasureSpec), getDefaultSize(getSuggestedMinimumHeight(),
                heightMeasureSpec));

        if (mOldWidth != getMeasuredWidth() || mOldHeight != getMeasuredHeight()) {
            onSizeChanged();
        }
    }

    /***
     * 重新计算一波尺寸
     */
    private void onSizeChanged() {
        mContentWidth = getMeasuredWidth() - mPaddingLeft - mPaddingRight;
        mContentHeight = (int) ((getMeasuredHeight() - mPaddingTop - mPaddingBottom) * 0.95f);
        //字体大小
        int textSize = 1;
        textSize = (int) (sDesiredCandidatesSize * Constants.getDensity(getContext()));
        mCandidatesPaint.setTextSize(textSize);
        mFmiCandidates = mCandidatesPaint.getFontMetricsInt();
        while (mFmiCandidates.bottom - mFmiCandidates.top > mContentHeight) { //大了就缩小一个等级
            textSize--;
            mCandidatesPaint.setTextSize(textSize);
            mFmiCandidates = mCandidatesPaint.getFontMetricsInt();
        }
        mImeCandidateTextSize = textSize;
        mRecommendedCandidateTextSize = textSize * 4 / 5;
        //如果已经又decodeInfo了直接从里面读
        if (null == mDecInfo) {
            mCurCandidateTextSize = mImeCandidateTextSize;
            mCandidatesPaint.setTextSize(mCurCandidateTextSize);
            mFmiCandidates = mCandidatesPaint.getFontMetricsInt();
//            mCurCandidateTextSize = mCandidatesPaint.measureText(SUSPENSION_POINTS);
        } else {
            setDecodingInfo(mDecInfo);
        }

        // When the size is changed, the first page will be displayed.
//        mPageNo = 0;
        resetAll();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null == mDecInfo || mDecInfo.isCandidatesListEmpty()) return;
        //更新下候选词
        //注意判定可以加个缓存
        updateTheCandidateRect();
        drawTheCandidates(canvas);
    }

    /**
     * 我考虑是增量增加
     * 还是全部覆盖增加
     * 直接增量吧
     */
    private void updateTheCandidateRect() {
        log("before rects.size : " + mCandidateRects.size() + "  candidates size : " + mDecInfo.mCandidatesList.size());
        int curSize = mCandidateRects.size();
        if (curSize > mDecInfo.mCandidatesList.size()) { //说明奇怪的问题发生导致错误
            MYLOG.LOGW("cursize > canlist, reset all");
            curSize = 0;
            mCandidateRects.clear();
        }
        float xPos = 0;
        float yPos = (getMeasuredHeight() - (mFmiCandidates.bottom - mFmiCandidates.top)) / 2 - mFmiCandidates.top;
        RectF firstRectF = mCandidateRects.size() > 0 ? mCandidateRects.get(mCandidateRects.size() - 1) : null;
        xPos = firstRectF == null ? mPaddingLeft : firstRectF.right;
        float theItemWidth = 0;

        List<String> cands = mDecInfo.mCandidatesList;
        for (int i = curSize; i < cands.size(); i++) {
            String theText = cands.get(i);
            theItemWidth = 2 * mCandidateMargin + mCandidatesPaint.measureText(theText);
            RectF rectF = new RectF(xPos, yPos + mFmiCandidates.top, xPos + theItemWidth, yPos + mFmiCandidates.bottom);
            mCandidateRects.add(rectF);
            xPos += theItemWidth;
        }
        log("after rects.size : " + mCandidateRects.size() + "  candidates size : " + cands.size());

    }

    /***
     * 先无论如何都画
     * 之后依旧是优化做缓存
     * @param canvas
     */
    private void drawTheCandidates(Canvas canvas) {
//        if (!isReDrawCandidate) return;
        if (null == mCandidateRects || mCandidateRects.size() == 0) return;
        float yPos = (getMeasuredHeight() - (mFmiCandidates.bottom - mFmiCandidates.top)) / 2 - mFmiCandidates.top;
        for (int i = 0; i < mCandidateRects.size(); i++) {
            RectF rect = mCandidateRects.get(i);
            float xPos = rect.left;
            if (i == mGlobalItemIndex) {
                mActiveCellDrawable.setBounds((int)rect.left, (int)rect.top,
                        (int)rect.right,(int) rect.bottom);
                mActiveCellDrawable.draw(canvas);
                mCandidatesPaint.setColor(mActiveCandidateColor);
            } else {
                mCandidatesPaint.setColor(mNormalCandidateColor);
            }
            xPos += mCandidateMargin;
            if (i == 0) {//之后看看要不要移出去不用每次都判断
                drawFirstSuitableText(canvas, (int) xPos, (int) yPos, mDecInfo.mCandidatesList.get(i));
            } else {
                canvas.drawText(mDecInfo.mCandidatesList.get(i), (int)xPos, (int)yPos, mCandidatesPaint);
            }
        }
//        isReDrawCandidate = false;
    }

    /***
     * 调整第一个候选词的大小防止过大
     * @param canvas
     * @param x
     * @param y
     * @param s
     */
    private void drawFirstSuitableText(Canvas canvas, int x, int y , String s) {
        float measureText = mCandidatesPaint.measureText(s);
        float textSize = mCandidatesPaint.getTextSize();
        float oriSize = textSize;
//        log("draw suitable size before : " + textSize);
        while (measureText > mContentWidth) {
            textSize--;
            mCandidatesPaint.setTextSize(textSize);
            measureText = mCandidatesPaint.measureText(s);
        }
        float minSize = sMinCandidatesSize * Constants.getDensity(getContext());
        textSize = textSize > minSize ? textSize : minSize;
        mCandidatesPaint.setTextSize(textSize);
//        log("draw suitable size after : " + textSize + " min is " + minSize);
        canvas.drawText(s, x, y, mCandidatesPaint);
        mCandidatesPaint.setTextSize(oriSize);//恢复现场
    }

    /***
     * 这个的话不做处理 真正的处理在下面 交给外面控制
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean onTouchEventReal(MotionEvent event) {
        if (null == mDecInfo.mCandidatesList || 0 == mDecInfo.mCandidatesList.size()) {
            resetAll();
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            int dx = (int) (mOriX - x);
            if (Math.abs(dx) < 10) {//忽略细小的移动
//                mIsSliding = false;
                return true;
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOriX = event.getX();
                mOriY = event.getY();
                mGlobalItemIndex = mapClickItem(mOriX + mTotalMoveX, mOriY);

                if (mGlobalItemIndex == -1) {
                    log("global index is -1 ! error");
                } else { //normal
                    Toast.makeText(getContext(), "click " + mDecInfo.mCandidatesList.get(mGlobalItemIndex), Toast.LENGTH_SHORT).show();
                }
                mIsSliding = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                int dx = (int) (mOriX - x);
                scrollBy(dx, 0);
                mTotalMoveX += dx;
                mOriX = x;
                mOriY = y;
                mIsSliding = true;
                log("is sliding");
//                mGlobalItemIndex = mapClickItem(oriX + mTotalMoveX, oriY);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsSliding) {
                    break;
                } else {
                    resetAll();
                    mCvListener.onClickChoice(mGlobalItemIndex);
                    mIsSliding = false;
                }
                log("action up oriX ======= : " + mOriX + "the total X :" + mTotalMoveX);
                break;
            case MotionEvent.ACTION_CANCEL:
                log("action up cancel ======= : " + mOriX + "the total X :" + mTotalMoveX);
                break;
        }
        return true;
    }

    private int mapClickItem(float x, float y) {
        int index = -1;
        float nearestDis = Float.MAX_VALUE;
        for (int i = 0; i < mCandidateRects.size(); i++) {
            RectF rect = mCandidateRects.get(i);
            if (rect.left <= x && x < rect.right
                    && rect.top <= y && y < rect.bottom) {
                return i;
            } else { //计算最近的 先不用 没有就没有
//                float disX = (rect.left + rect.right) / 2;
//                float disY = (rect.top + rect.bottom) / 2;
//                float dis = disX * disX + disY * disY;
//                if (dis < nearestDis) {
//                    index = i;
//                }
            }
        }
        return index;
    }

    private void log(String str) {
        MYLOG.LOGI(str);
    }

    public void enableActiveHighlight(boolean enableActiveHighlight) {
//        mEnableActiveHighlight = enableActiveHighlight;
        invalidate();
    }

    public int getGlobalIndex() {
        return mGlobalItemIndex;
    }

    public void resetAll() {
        log("reset all");
        mGlobalItemIndex = 0;
        mTotalMoveX = 0;
        mActiveCandInPage = 0;
        mCandidateRects.clear();
        log("the canRect size " + mCandidateRects.size());
        invalidate();
    }

    public void setIsRedrawKey(boolean isUpdate) {

    }

    //todo 之后会自定义观察者接口
    @Override
    public void update(Observable observable, Object data) {
        log("update observer");
        resetAll();
    }

//    public void set(boolean loadMoreFlag) {
//        this.mLoadMoreFlag = loadMoreFlag;
//    }
}
