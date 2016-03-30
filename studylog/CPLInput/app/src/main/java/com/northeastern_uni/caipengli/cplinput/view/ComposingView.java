package com.northeastern_uni.caipengli.cplinput.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.R;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;

/**
 * Created by caipengli on 16年3月10日.
 * 这个是用于显示拼音拼写的view
 * todo 之后这个将会优化成可以拖动的形式
 */
public class ComposingView extends View {

    private static final int LEFT_RIGHT_MARGIN = 5;

    protected int mPaddingLeft = InputEnvironment.LEFT_PADDING;
    protected int mPaddingRight = InputEnvironment.RIGHT_PADDING;
    protected int mPaddingTop = InputEnvironment.TOP_PADDING;
    protected int mPaddingBottom = InputEnvironment.BOTTOM_PADDING;

    private Paint mPaint;
    private Drawable mHlDrawable;
    private Drawable mCursor;//光标
    private Paint.FontMetricsInt mFmi;

    private int mStrColor;
    private int mStrColorHl;
    private int mStrColorIdle; //空闲状态的颜色

    private int mFontSize;
    private ComposingStatus mStatus;
    CPLIME.DecodingInfo mDecInfo;

    public enum ComposingStatus {
        /**
         * 这个是正常状态
         */
        SHOW_PINYIN,
        /**
         * 这个是在中文输入法中强行输入英文 比如按了 v之后
         */
        SHOW_STRING_LOWERCASE,
        /**
         * 编辑已经那个输入的拼音
         * 如果用户点击左右的话就进入该模式来编译拼音
         */
        EDIT_PINYIN
    }

    public ComposingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources r = context.getResources();
        mHlDrawable = r.getDrawable(R.drawable.bg_candidate_background);
        mCursor = r.getDrawable(R.drawable.composing_area_cursor);

        mStrColor = r.getColor(R.color.composing_color);
        mStrColorHl = r.getColor(R.color.composing_color_hl);
        mStrColorIdle = r.getColor(R.color.composing_color_idle);
        mFontSize = r.getDimensionPixelSize(R.dimen.composing_height);

        mPaint = new Paint();
        mPaint.setColor(mStrColor);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mFontSize);
        mFmi = mPaint.getFontMetricsInt();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = 0;
        int height = mFmi.bottom - mFmi.top + mPaddingTop + mPaddingBottom;
        if (null == mDecInfo) {
            width = 0;
        } else {
            width = mPaddingLeft + mPaddingRight + LEFT_RIGHT_MARGIN * 2;
            String str;
            if (mStatus == ComposingStatus.SHOW_STRING_LOWERCASE) {
                str = mDecInfo.getOriginalSplStr().toString();
            } else {
                str = mDecInfo.getComposingStrForDisplay();
            }
            width += mPaint.measureText(str, 0, str.length());
        }
        setMeasuredDimension((int) (width + 0.5f), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ComposingStatus.EDIT_PINYIN == mStatus || ComposingStatus.SHOW_PINYIN == mStatus) {
            drawForPinyin(canvas);
            return;
        }
        //强行英文 <- 无候选词 比如一开就点了v
        float x = 0;
        float y = 0;
        x = mPaddingLeft + LEFT_RIGHT_MARGIN;
        y = 0 + mPaddingTop - mFmi.top;

        mPaint.setColor(mStrColorHl);
        mHlDrawable.setBounds(mPaddingLeft, mPaddingTop, getWidth()
                - mPaddingRight, getHeight() - mPaddingBottom);
        mHlDrawable.draw(canvas);
        String splStr = mDecInfo.getOriginalSplStr().toString();
        canvas.drawText(splStr, 0, splStr.length(), x, y, mPaint);
    }

    /**
     * 这个的话是拼音状态下输入
     * @param canvas
     */
    private void drawForPinyin(Canvas canvas) {
//        MYLOG.LOGD("draw pinyin composing : " + mDecInfo.getComposingStr());
//        MYLOG.LOGD("draw pinyin original : " + mDecInfo.getOriginalSplStr());
//        MYLOG.LOGD("draw pinyin active part : " + mDecInfo.getComposingStrActivePart());
//        MYLOG.LOGD("draw pinyin for display : " + mDecInfo.getComposingStrForDisplay());
        float x = 0;
        float y = 0;
        x = mPaddingLeft + LEFT_RIGHT_MARGIN;
        y = 0 + mPaddingTop - mFmi.top;
        mPaint.setColor(mStrColor);
        int cursorPos = mDecInfo.getCursorPosInCmpsDisplay();
        int cmpsPos = cursorPos;
        int activeCmpsLen = mDecInfo.getActiveCmpsDisplayLen();
        if (cursorPos > activeCmpsLen) cmpsPos = activeCmpsLen;

        String cmpsStr = mDecInfo.getComposingStrForDisplay();
        canvas.drawText(cmpsStr, 0, cmpsPos, x, y, mPaint);
        x += mPaint.measureText(cmpsStr, 0, cmpsPos);//先量出光标前的拼音长度

        if (cursorPos <= activeCmpsLen) {//
            if (ComposingStatus.EDIT_PINYIN == mStatus) { //说明现在编辑的地方不在最后
                drawCursor(canvas, x);//画光标
            }
            canvas.drawText(cmpsStr, cmpsPos, activeCmpsLen, x, y, mPaint);
        }
        x += mPaint.measureText(cmpsStr, cmpsPos, activeCmpsLen);//所有输入有效字母总长度

        if (activeCmpsLen < cmpsStr.length()) {//说明有部分的是无效的不能识别有误的拼音
            mPaint.setColor(mStrColorIdle);
            int oriPos = activeCmpsLen;
            if (cursorPos > activeCmpsLen) {
                if (cursorPos > cmpsStr.length()) {
                    cursorPos = cmpsStr.length();
                }
                canvas.drawText(cmpsStr, oriPos, cursorPos, x, y, mPaint);
                x += mPaint.measureText(cmpsStr, oriPos, cursorPos);
                if (ComposingStatus.EDIT_PINYIN == mStatus) {
                    drawCursor(canvas, x);
                }
                oriPos = cursorPos;
            }
            canvas.drawText(cmpsStr, oriPos, cmpsStr.length(), x, y, mPaint);
        }

    }

    private void drawCursor(Canvas canvas, float x) {
        mCursor.setBounds((int) x, mPaddingTop, (int) x
                + mCursor.getIntrinsicWidth(), getHeight() - mPaddingBottom);
        mCursor.draw(canvas);
    }

    public void reset() {
        mStatus = ComposingStatus.SHOW_PINYIN;
    }

    public void setDecodingInfo (CPLIME.DecodingInfo decodingInfo, CPLIME.ImeState imeState) {
        mDecInfo = decodingInfo;
        if (CPLIME.ImeState.STATE_INPUT == imeState) {
            mStatus = ComposingStatus.SHOW_PINYIN;
            mDecInfo.moveCursorToEdge(false);
        } else {
            if (0 != decodingInfo.getFixedLen() || ComposingStatus.EDIT_PINYIN == mStatus) {
                mStatus = ComposingStatus.EDIT_PINYIN;
            } else { //如果没有或选词或者 状态不为edit pinyin
                mStatus = ComposingStatus.SHOW_STRING_LOWERCASE;
            }
            mDecInfo.moveCursor(0);
        }
        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        requestLayout();
        invalidate();
    }

    public boolean moveCursor(int keyCode) {
        if (KeyEvent.KEYCODE_DPAD_RIGHT != keyCode
                && KeyEvent.KEYCODE_DPAD_LEFT != keyCode) { //这个主要是考虑到物理按键
            return false;
        }
        if (ComposingStatus.EDIT_PINYIN == mStatus) {
            int offset = 0;
            if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
                offset = -1;
            } else {
                offset = 1;
            }
            mDecInfo.moveCursor(offset);
        } else if (ComposingStatus.SHOW_STRING_LOWERCASE == mStatus) {
            mStatus = ComposingStatus.EDIT_PINYIN;
            measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            requestLayout();
        }
        invalidate();
        return true;
    }

    public ComposingStatus getComposingStatus() {
        return mStatus;
    }
}
