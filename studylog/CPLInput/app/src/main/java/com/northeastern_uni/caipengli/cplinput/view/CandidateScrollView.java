package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.logic.CandidatesListener;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.R;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * Created by caipengli on 16年3月9日.
 * 可滑动的候选词窗口
 */
public class CandidateScrollView extends View implements Observer {

    /***
     * 如果滑到了最后就留一定的空白告诉用户已经是最后了
     */
    private static int sBlankInTheEnd = 30;

    /**
     * 预期的字体大小 dp
     * 从配置文件中读取
     */
    private static int sDesiredCandidatesSize = 22;
    private static int sMinCandidatesSize = 18;


    protected int mPaddingLeft = InputEnvironment.LEFT_PADDING;
    protected int mPaddingRight = InputEnvironment.RIGHT_PADDING;
    protected int mPaddingTop = InputEnvironment.TOP_PADDING;
    protected int mPaddingBottom = InputEnvironment.BOTTOM_PADDING;
    private int mContentWidth;
    private int mContentHeight;

    private ArrowUpdater mArrowUpdater;
    private CandidatesListener mCvListener;

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
    private RectF mActiveCellRect;
    private float mOriX;
    private float mOriY;
    private boolean mIsSliding = false;

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
    private CPLIME.DecodingInfo mDecInfo;

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
        sBlankInTheEnd = (int) (sBlankInTheEnd * InputEnvironment.getDensity(context));

        mCandidatesPaint = new Paint();
        mCandidatesPaint.setAntiAlias(true);
        mActiveCellRect = new RectF();

    }

    public void initialize(ArrowUpdater arrowUpdater, /*BalloonHint balloonHint,*/
                           CandidatesListener cvListener) {
        mArrowUpdater = arrowUpdater;
        mCvListener = cvListener;
    }

    public void setDecodingInfo(CPLIME.DecodingInfo decInfo) {
        if (null == decInfo) return;
        mDecInfo = decInfo;
        mDecInfo.addObserver(this);

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
        }

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
        textSize = (int) (sDesiredCandidatesSize * InputEnvironment.getDensity(getContext()));
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

        resetAll();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null == mDecInfo || mDecInfo.isCandidatesListEmpty()) return;
        //更新下候选词
        updateTheCandidateRect();
        drawTheCandidates(canvas);
    }

    /**
     * 我考虑是增量增加
     * 还是全部覆盖增加
     * 直接增量吧
     */
    private void updateTheCandidateRect() {
        //log("before rects.size : " + mCandidateRects.size() + "  candidates size : " + mDecInfo.mCandidatesList.size());
        int curSize = mCandidateRects.size();
        if (curSize > mDecInfo.mCandidatesList.size()) { //说明奇怪的问题发生导致错误
            MYLOG.LOGW("cursize > canlist, reset all");
            curSize = 0;
            mCandidateRects.clear();
        }
        float xPos = 0;
        RectF firstRectF = mCandidateRects.size() > 0 ? mCandidateRects.get(mCandidateRects.size() - 1) : null;
        xPos = firstRectF == null ? mPaddingLeft : firstRectF.right;

        float theItemWidth = 0;
        List<String> cands = mDecInfo.mCandidatesList;
        for (int i = curSize; i < cands.size(); i++) {
            String theText = cands.get(i);
            float textWidth = mCandidatesPaint.measureText(theText);
            theItemWidth = 2 * mCandidateMargin + textWidth;
            //log("itemwidth : " + theItemWidth + "  the margin : " + mCandidateMargin);
            RectF rectF = new RectF(xPos, 0 /*+ mFmiCandidates.top*/, xPos + theItemWidth, getMeasuredHeight());
            mCandidateRects.add(rectF);
            xPos += theItemWidth;
        }
        //log("after rects.size : " + mCandidateRects.size() + "  candidates size : " + cands.size());

    }

    /***
     * 先无论如何都画
     * todo 这个之后就是做缓存的优化了　然而比较难
     * @param canvas
     */
    private void drawTheCandidates(Canvas canvas) {
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
        checkTheArrowState();
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
        //log("draw suitable size before : " + textSize);
        while (measureText > mContentWidth) {
            textSize--;
            mCandidatesPaint.setTextSize(textSize);
            measureText = mCandidatesPaint.measureText(s);
        }
        float minSize = sMinCandidatesSize * InputEnvironment.getDensity(getContext());
        textSize = textSize > minSize ? textSize : minSize;
        mCandidatesPaint.setTextSize(textSize);
        //log("draw suitable size after : " + textSize + " min is " + minSize);
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

    //// TO DO: 16年3月9日 然后要做的就是滑动限制 和 滑动过一半自动加载 //已经完成
    public boolean onTouchEventReal(MotionEvent event) {

        if (null == mDecInfo
                || null == mDecInfo.mCandidatesList
                || 0 == mDecInfo.mCandidatesList.size()) {
            resetAll();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            int dx = (int) (mOriX - x);
            if (Math.abs(dx) < 10) {//忽略细小的移动
                return true;
            }
            /**下面是边界限制如果滑到了边界的话就不允许再滑*/
            if (mTotalMoveX <= 0 && dx < 0) {
                mIsSliding = true;//手指还在滑
                return true;//说明是开头了不准再左滑
            }
            if (mTotalMoveX + mContentWidth
                    >= mCandidateRects.get(mCandidateRects.size() - 1).right + sBlankInTheEnd //稍微多点用户体验好些
                    && dx > 0) {
                mIsSliding = true;
                return true;//说明是到尾巴了不能再滑
            }
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOriX = event.getX();
                mOriY = event.getY();
                mGlobalItemIndex = mapClickItem(mOriX + mTotalMoveX, mOriY);
                if (mGlobalItemIndex == -1) {
                    MYLOG.LOGW("global index is -1 ! error");
                } else { //normal
                }
                invalidate();
                mIsSliding = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                int dx = (int) (mOriX - x);
                scrollBy(dx, 0);
                checkCenterIndex();
                mTotalMoveX += dx;
                mOriX = x;
                mOriY = y;
                mIsSliding = true;
//                mGlobalItemIndex = mapClickItem(oriX + mTotalMoveX, oriY);
                break;
            case MotionEvent.ACTION_UP:
                if (mIsSliding) {
                    checkTheArrowState();
                    break;
                } else {
//                    MYLOG.LOGI("now choice : " + mGlobalItemIndex);
                    mCvListener.onClickChoice(mGlobalItemIndex);
                    resetAll();
                    mIsSliding = false;
                }
//                log("action up oriX ======= : " + mOriX + "the total X :" + mTotalMoveX);
                break;
            case MotionEvent.ACTION_CANCEL:
//                log("action up cancel ======= : " + mOriX + "the total X :" + mTotalMoveX);
                break;
        }
        return true;
    }

    public void checkTheArrowState() {
        mArrowUpdater.updateArrowStatus(isCanScrollBackward(), isCanScrollForward());
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

//    public void enableActiveHighlight(boolean enableActiveHighlight) {
//        mEnableActiveHighlight = enableActiveHighlight;
//        invalidate();
//    }

    public int getGlobalIndex() {
        return mGlobalItemIndex;
    }

    /**
     * 点击翻页按钮的那种翻页
     */
    public void scrollForward() {
        float willTarget = mTotalMoveX + 2 * mContentWidth;//这个是显示的地方
        if (mCandidateRects.size() <= 0) return;
        RectF lastRect = mCandidateRects.get(mCandidateRects.size() - 1);
        if (willTarget > lastRect.right + sBlankInTheEnd) { //如果强行翻页已经是最后了的话
            int dx= (int) ((lastRect.right + sBlankInTheEnd) - (mTotalMoveX + mContentWidth));//只能到最后
            scrollBy(dx, 0);
            mTotalMoveX += dx;
        } else {
            scrollBy(mContentWidth, 0);
            mTotalMoveX += mContentWidth;//直接翻到下页
        }
        checkCenterIndex();
    }

    /**
     * 点击向前翻页
     */
    public void scrollBackWard() {
        float willTraget = mTotalMoveX - mContentWidth;
        if (willTraget > 0) {
            scrollBy(-mContentWidth, 0);
            mTotalMoveX -= mContentWidth;
        } else {
            mTotalMoveX = 0;
            scrollTo(0, 0);
        }
    }

    /**
     * 重置所有
     */
    public void resetAll() {
        log("reset all");
        mGlobalItemIndex = 0;
        mTotalMoveX = 0;
        mActiveCandInPage = 0;
        mCandidateRects.clear();
        scrollTo(0, 0);//回到最初
        invalidate();
    }

    @Override
    public void update(Observable observable, Object data) {
        boolean restAll = data != null ? (boolean) data : true;
        if (restAll) {
            resetAll();
        } else {
            invalidate();//仅仅看看数据有没变
        }
    }

    /**
     * 获取屏幕中心的那个候选词的序号
     * 如果还差指定的(32)个就到头了就加载下一版
     * @return
     */
    private void checkCenterIndex() {
        if (null == mDecInfo || mCandidateRects.size() <= 0) return;

        float centerX = mTotalMoveX + mContentWidth / 2;
        int index = mapClickItem(centerX, getHeight() / 2);
        if (index == -1) {
            return;
        }
        if (index + InputEnvironment.LOAD_SEQUENT_SIZE >= mCandidateRects.size()) {
//            log("need load more now candidate size" + mCandidateRects.size());
            mDecInfo.loadMore(false);
        }
    }

    /**
     *
     * @return 能否往后一页翻
     */
    public boolean isCanScrollForward() {
        if (null == mCandidateRects || mCandidateRects.size() == 0) return false;
        return mTotalMoveX + mContentWidth < mCandidateRects.get(mCandidateRects.size() - 1).left;
    }

    /**
     * return 能否往前一页翻
     */
    public boolean isCanScrollBackward() {
        return mTotalMoveX > mCandidateMargin;//有一点点的margin
    }



}
