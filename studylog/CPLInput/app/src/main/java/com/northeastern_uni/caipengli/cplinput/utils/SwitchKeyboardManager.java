package com.northeastern_uni.caipengli.cplinput.utils;

import android.content.res.Resources;
import android.view.inputmethod.EditorInfo;

import com.northeastern_uni.caipengli.cplinput.CPLIME;
import com.northeastern_uni.caipengli.cplinput.logic.SoftKeyboard;
import com.northeastern_uni.caipengli.cplinput.R;

/**
 * Created by caipengli on 16年2月25日.
 */
public class SwitchKeyboardManager {

    private static final int USERDEF_KEYCODE_SHIFT_1 = -1;
    private static final int USERDEF_KEYCODE_LANG_2 = -2; //切换语言
    private static final int USERDEF_KEYCODE_SYM_3 = -3;
    private static final int USERDEF_KEYCODE_PHONE_SYM_4 = -4;
    private static final int USERDEF_KEYCODE_MORE_SYM_5 = -5;
    private static final int USERDEF_KEYCODE_SMILEY_6 = -6;
    //第八为为键盘类型为 0则不需要调出软键盘
    private static final int MASK_SKB_LAYOUT = 0xf0000000; //1111000...
    private static final int MASK_SKB_LAYOUT_QWERTY = 0x10000000;
    private static final int MASK_SKB_LAYOUT_SYMBOL1 = 0x20000000;
    private static final int MASK_SKB_LAYOUT_SYMBOL2 = 0x30000000;
    private static final int MASK_SKB_LAYOUT_SMILEY = 0x40000000;
    private static final int MASK_SKB_LAYOUT_PHONE = 0x50000000;
    //第七位为语言
    private static final int MASK_LANGUAGE = 0x0f000000;
    private static final int MASK_LANGUAGE_CN = 0x01000000;
    private static final int MASK_LANGUAGE_EN = 0x02000000;
    //第六位为大小写
    private static final int MASK_CASE = 0x00f00000;
    private static final int MASK_CASE_LOWER = 0x00100000;
    private static final int MASK_CASE_UPPER = 0x00200000;
    //中文软键盘 和 中文符号键盘两种
    public static final int MODE_SKB_CHINESE = (MASK_SKB_LAYOUT_QWERTY | MASK_LANGUAGE_CN);
    public static final int MODE_SKB_SYMBOL1_CN = (MASK_SKB_LAYOUT_SYMBOL1 | MASK_LANGUAGE_CN);
    public static final int MODE_SKB_SYMBOL2_CN = (MASK_SKB_LAYOUT_SYMBOL2 | MASK_LANGUAGE_CN);
    //英文键盘大小写 和 英文符号键盘
    public static final int MODE_SKB_ENGLISH_LOWER = (MASK_SKB_LAYOUT_QWERTY
            | MASK_LANGUAGE_EN | MASK_CASE_LOWER);
    public static final int MODE_SKB_ENGLISH_UPPER = (MASK_SKB_LAYOUT_QWERTY
            | MASK_LANGUAGE_EN | MASK_CASE_UPPER);
    public static final int MODE_SKB_SYMBOL1_EN = (MASK_SKB_LAYOUT_SYMBOL1 | MASK_LANGUAGE_EN);
    public static final int MODE_SKB_SYMBOL2_EN = (MASK_SKB_LAYOUT_SYMBOL2 | MASK_LANGUAGE_EN);
    //其他键盘类型
    public static final int MODE_SKB_SMILEY = (MASK_SKB_LAYOUT_SMILEY | MASK_LANGUAGE_CN);
    public static final int MODE_SKB_PHONE_NUM = (MASK_SKB_LAYOUT_PHONE);
    public static final int MODE_SKB_PHONE_SYM = (MASK_SKB_LAYOUT_PHONE | MASK_CASE_UPPER);
    //物理键盘输入s
    public static final int MODE_HKB_CHINESE = (MASK_LANGUAGE_CN);
    public static final int MODE_HKB_ENGLISH = (MASK_LANGUAGE_EN);
    //其他
    public static final int MODE_UNSET = 0;//未设置
    public static final int MAX_TOGGLE_STATES = 4;//最多的变化状态4

    private int mInputMode = MODE_UNSET;
    /**用于记住上次的输入法模式,这次输入会首先尝试上次的输入法模式是否合适*/
    private int mPreviousInputMode = MODE_SKB_CHINESE;//默认之前的是中文输入法
    private int mRecentLauageInputMode = MODE_SKB_CHINESE;

    private EditorInfo mEditorInfo;
    private ToggleStates mToggleStates = new ToggleStates();
    //是否会显示表情 Todo : 以后将会修改到工具栏
    private boolean mShorMessageField;
    private boolean mEnterKeyNormal = true;

    int mInputIcon = R.drawable.ime_pinyin;
    private CPLIME mImeService;//Todo : core cpl
    /**以下为各个不同状态下的键盘*/
    private int mToggleStateCn;
    private int mToggleStateCnCandidates;
    private int mToggleStateEnLower;
    private int mToggleStateEnUpper;
    private int mToggleStateEnSym1;
    private int mToggleStateEnSym2;
    private int mToggleStateSmiley;
    private int mToggleStatePhoneSym;
    private int mToggleStateGo;//回车键 为 Go
    private int mToggleStateSearch;//回车键 为 Search
    private int mToggleStateSend;//回车键 为 Send
    private int mToggleStateNext;//回车键 为 Next
    private int mToggleStateDone;//回车键 为 Done
    private int mToggleRowCn;
    private int mToggleRowEn;
    private int mToggleRowUri;//uri模式方便输入网址
    private int mToggleRowEmailAddress;

    public SwitchKeyboardManager(CPLIME imeService) {
        mImeService = imeService;//// TODO: 16年2月25日
        Resources r = mImeService.getResources();
        mToggleStateCn = Integer.parseInt(r.getString(R.string.toggle_cn));
        mToggleStateCnCandidates = Integer.parseInt(r.getString(R.string.toggle_cn_cand));
        mToggleStateEnLower = Integer.parseInt(r.getString(R.string.toggle_en_lower));
        mToggleStateEnUpper = Integer.parseInt(r.getString(R.string.toggle_en_upper));
        mToggleStateEnSym1 = Integer.parseInt(r.getString(R.string.toggle_en_sym1));
        mToggleStateEnSym2 = Integer.parseInt(r.getString(R.string.toggle_en_sym2));
        mToggleStateSmiley = Integer.parseInt(r.getString(R.string.toggle_smiley));
        mToggleStatePhoneSym = Integer.parseInt(r.getString(R.string.toggle_phone_sym));

        mToggleStateGo = Integer.parseInt(r.getString(R.string.toggle_enter_go));
        mToggleStateSearch = Integer.parseInt(r.getString(R.string.toggle_enter_search));
        mToggleStateSend = Integer.parseInt(r.getString(R.string.toggle_enter_send));
        mToggleStateNext = Integer.parseInt(r.getString(R.string.toggle_enter_next));
        mToggleStateDone = Integer.parseInt(r.getString(R.string.toggle_enter_done));

        mToggleRowCn = Integer.parseInt(r.getString(R.string.toggle_row_cn));
        mToggleRowEn = Integer.parseInt(r.getString(R.string.toggle_row_en));
        mToggleRowUri = Integer.parseInt(r.getString(R.string.toggle_row_uri));
        mToggleRowEmailAddress = Integer.parseInt(r.getString(R.string.toggle_row_emailaddress));
    }

    public int getSkbLayout() {
        int layout = (mInputMode & MASK_SKB_LAYOUT);

        switch (layout) {
            case MASK_SKB_LAYOUT_QWERTY:
                return R.xml.skb_qwerty;
            case MASK_SKB_LAYOUT_SYMBOL1:
                return R.xml.skb_sym1;
            case MASK_SKB_LAYOUT_SYMBOL2:
                return R.xml.skb_sym2;
            case MASK_SKB_LAYOUT_SMILEY:
                return R.xml.skb_smiley;
            case MASK_SKB_LAYOUT_PHONE:
                return R.xml.skb_phone;
        }
        return 0;
    }

    public int switchLanguageWithHkb() {
        int newInoutMode = MODE_HKB_CHINESE;
        mInputIcon = R.drawable.ime_pinyin;
        if (MODE_HKB_CHINESE == mInputMode) {
            newInoutMode = MODE_HKB_ENGLISH;
            mInputIcon = R.drawable.ime_en;
        }
        saveInputMode(newInoutMode);
        return  mInputIcon;
    }

    /**
     * 这个是点击特殊按键的键盘转换
     * @param userKey
     * @return
     */
    public int switchModeForUserKey(int userKey) {
        int newInputMode = MODE_UNSET;
        if (USERDEF_KEYCODE_LANG_2 == userKey) { //点击的是切换语言
            if (MODE_SKB_CHINESE == mInputMode) {
                newInputMode = MODE_SKB_ENGLISH_LOWER;
            } else if (MODE_SKB_ENGLISH_LOWER == mInputMode || MODE_SKB_ENGLISH_UPPER == mInputMode) {
                newInputMode = MODE_SKB_CHINESE;
            } else if (MODE_SKB_SYMBOL1_CN == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL1_EN;
            } else if (MODE_SKB_SYMBOL2_CN == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL2_EN;
            } else if (MODE_SKB_SYMBOL1_EN == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL1_CN;
            } else if (MODE_SKB_SYMBOL2_EN == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL2_CN;
            } else if (MODE_SKB_SMILEY == mInputMode) {
                newInputMode = MODE_SKB_CHINESE;
            }
        } else  if (USERDEF_KEYCODE_SYM_3 == userKey) { //点击的是符号按钮
            if (MODE_SKB_CHINESE == mInputMode) { //正常转符号
                newInputMode = MODE_SKB_SYMBOL1_CN;
            } else if (MODE_SKB_ENGLISH_UPPER == mInputMode
                    || MODE_SKB_ENGLISH_LOWER == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL1_EN;
            } else if (MODE_SKB_SYMBOL1_EN == mInputMode
                    || MODE_SKB_SYMBOL2_EN == mInputMode) { // 符号转正常
                newInputMode = MODE_SKB_ENGLISH_LOWER;
            } else if (MODE_SKB_SYMBOL1_CN == mInputMode
                    || MODE_SKB_SYMBOL2_CN == mInputMode) {
                newInputMode = MODE_SKB_CHINESE;
            } else if (MODE_SKB_SMILEY == mInputMode) {
                newInputMode = MODE_SKB_SYMBOL1_CN;
            }
        } else if (USERDEF_KEYCODE_SHIFT_1 == userKey) { // 点的是shift
            if (MODE_SKB_ENGLISH_LOWER == mInputMode) {
                newInputMode = MODE_SKB_ENGLISH_UPPER;
            } else if (MODE_SKB_ENGLISH_UPPER == mInputMode) {
                newInputMode = MODE_SKB_ENGLISH_LOWER;
            }
        } else if (USERDEF_KEYCODE_MORE_SYM_5 == userKey) {
            int sym = (MASK_SKB_LAYOUT & mInputMode);
            if (MASK_SKB_LAYOUT_SYMBOL1 == sym) {
                sym = MASK_SKB_LAYOUT_SYMBOL2;
            } else {
                sym = MASK_SKB_LAYOUT_SYMBOL1;
            }
            newInputMode = ((mInputMode & (~MASK_SKB_LAYOUT)) | sym);
        } else if (USERDEF_KEYCODE_SMILEY_6 == userKey) {
            if (MODE_SKB_CHINESE == mInputMode) {
                newInputMode = MODE_SKB_SMILEY;
            } else {
                newInputMode = MODE_SKB_CHINESE;
            }
        } else if (USERDEF_KEYCODE_PHONE_SYM_4 == userKey) {
            if (MODE_SKB_PHONE_NUM == mInputMode) {
                newInputMode = MODE_SKB_PHONE_SYM;
            } else {
                newInputMode = MODE_SKB_PHONE_NUM;
            }
        }

        if (newInputMode == mInputMode || MODE_UNSET == newInputMode) {
            return mInputIcon;
        }
        saveInputMode(newInputMode);
        prepareToggleStates(true);
        return newInputMode;
    }

    private void saveInputMode(int newInoutMode) {
        mPreviousInputMode = mInputMode;
        mInputMode = newInoutMode;
        int skbLayout = mInputMode & MASK_SKB_LAYOUT;
        if (MASK_SKB_LAYOUT_QWERTY == skbLayout || MODE_UNSET == skbLayout) {
            mRecentLauageInputMode = mInputMode;
        }
        mInputIcon = R.drawable.ime_pinyin;
        if (isEnglishWithHkb()) {
            mInputIcon = R.drawable.ime_en;
        } else if (isChineseTextWithHkb()) {
            mInputIcon = R.drawable.ime_pinyin;
        }
        if (!InputEnvironment.getInstance().hasHardKeyboard()) {
            mInputIcon = 0;
        }
    }

    /***
     * 这个boolean是为了防止有可能是物理键盘不是软键盘
     * @param needSkb
     */
    private void prepareToggleStates(boolean needSkb) {
        mEnterKeyNormal = true;
        if (!needSkb) return;

        mToggleStates.mQwerty = false;
        mToggleStates.mKeyStatesNum = 0;
        int states [] = mToggleStates.mKeyStates;
        int statesNum = mToggleStates.mKeyStatesNum;
        //目前要设定的
        int language = (mInputMode & MASK_LANGUAGE);
        int layout = (mInputMode & MASK_SKB_LAYOUT);
        int charcase = (mInputMode & MASK_CASE);
        /**下面这个属性主要是获取输入view的类型,比如可能他就要email啊number啊之类的*/
        int variation = mEditorInfo.inputType & EditorInfo.TYPE_MASK_VARIATION;

        if (MASK_SKB_LAYOUT_PHONE != layout) {
            if (MASK_LANGUAGE_CN == language) {
                if (MASK_SKB_LAYOUT_QWERTY == layout) {
                    mToggleStates.mQwerty = true;
                    mToggleStates.mQwertyUpperCase = true;
                    if (mShorMessageField) {
                        states[statesNum] = mToggleStateSmiley;
                        statesNum++;
                    }
                }
            } else if (MASK_LANGUAGE_EN == language) {
                if (MASK_SKB_LAYOUT_QWERTY == layout) { //英文下面的qwerty
                    mToggleStates.mQwerty = true;
                    mToggleStates.mQwertyUpperCase = false;
                    states[statesNum] = mToggleStateEnLower;
                    if (MASK_CASE_UPPER == charcase) { //大写再说
                        mToggleStates.mQwertyUpperCase = true;
                        states[statesNum] = mToggleStateEnUpper;
                    }
                    statesNum++;
                } else if (MASK_SKB_LAYOUT_SYMBOL1 == layout) {
                    states[statesNum] = mToggleStateEnSym1;
                    statesNum++;
                } else if (MASK_SKB_LAYOUT_SYMBOL2 == layout) {
                    states[statesNum] = mToggleStateEnSym2;
                    statesNum++;
                }
            }

            //由于非拼音用的都是26键盘所以拼音和英文下面一样
            mToggleStates.mRowIdToEnable = SoftKeyboard.KeyRow.DEFAULT_ROW_ID;
            if (EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS == variation) { //输入框要求email
                mToggleStates.mRowIdToEnable = mToggleRowEmailAddress;
            } else if (EditorInfo.TYPE_TEXT_VARIATION_URI == variation) {
                mToggleStates.mRowIdToEnable = mToggleRowUri;
            } else if (MASK_LANGUAGE_CN == language) {
                mToggleStates.mRowIdToEnable = mToggleRowCn;
            } else if (MASK_LANGUAGE_EN == language) {
                mToggleStates.mRowIdToEnable = mToggleRowEn;
            }
        } else {
            if (MASK_CASE_UPPER == charcase) {
                states[statesNum] = mToggleStatePhoneSym;
                statesNum++;
            }
        }

        //enter键的功能
        int action = mEditorInfo.imeOptions & (EditorInfo.IME_MASK_ACTION | EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        if (action == EditorInfo.IME_ACTION_GO) {
            states[statesNum] = mToggleStateGo;
            statesNum++;
            mEnterKeyNormal = false;
        } else if (action == EditorInfo.IME_ACTION_SEARCH) {
            states[statesNum] = mToggleStateSearch;
            statesNum++;
            mEnterKeyNormal = false;
        } else if (action == EditorInfo.IME_ACTION_SEND) {
            states[statesNum] = mToggleStateSend;
            statesNum++;
            mEnterKeyNormal = false;
        } else if (action == EditorInfo.IME_ACTION_NEXT) {
            int f = mEditorInfo.inputType & EditorInfo.TYPE_MASK_FLAGS;
            if (f != EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) { //如果允许多行的话就是换行而不是切换否则切换
                states[statesNum] = mToggleStateNext;
                statesNum++;
                mEnterKeyNormal = false;
            }
        } else if (action == EditorInfo.IME_ACTION_DONE) {
            states[statesNum] = mToggleStateDone;
            statesNum++;
            mEnterKeyNormal = false;
        }
        mToggleStates.mKeyStatesNum = statesNum;
    }

    public int requestInputWithHkb(EditorInfo editorInfo) {
        mShorMessageField = false;
        boolean english = false;
        int newInputMode = MODE_HKB_CHINESE;
        switch (editorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_PHONE:
            case EditorInfo.TYPE_CLASS_DATETIME:
                english = true;
                break;
            case EditorInfo.TYPE_CLASS_TEXT:
                int v = editorInfo.inputType & EditorInfo.TYPE_MASK_VARIATION;
                if (EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS == v
                        || EditorInfo.TYPE_TEXT_VARIATION_PASSWORD == v
                        || EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == v
                        || EditorInfo.TYPE_TEXT_VARIATION_URI == v) {
                    english = true;
                } else if (EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE == v) {
                        mShorMessageField = true;
                }
                break;
            default://保持之前的
        }
        if (english) {
            newInputMode = MODE_HKB_ENGLISH;
        } else {
            if ((mRecentLauageInputMode & MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                newInputMode = MODE_HKB_CHINESE;
            } else {
                newInputMode = MODE_HKB_ENGLISH;
            }
        }
        mEditorInfo = editorInfo;
        mEditorInfo.imeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;//显式设置 横屏的时候不全屏 cpl
        saveInputMode(newInputMode);
        prepareToggleStates(false);//应为是硬件盘所以酱紫
        return  mInputIcon;
    }

    public int requestInputWithSkb(EditorInfo editorInfo) {
        mShorMessageField = false;
        int newInputMode = MODE_HKB_CHINESE;
        switch (editorInfo.inputType & EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                newInputMode = MODE_SKB_SYMBOL1_EN;
                break;
            case EditorInfo.TYPE_CLASS_PHONE:
                newInputMode = MODE_SKB_PHONE_NUM;
                break;
            case EditorInfo.TYPE_CLASS_TEXT:
                int v = editorInfo.inputType & EditorInfo.TYPE_MASK_VARIATION;
                if (EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS == v
                        || EditorInfo.TYPE_TEXT_VARIATION_PASSWORD == v
                        || EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == v
                        || EditorInfo.TYPE_TEXT_VARIATION_URI == v) {
                    newInputMode = MODE_SKB_ENGLISH_LOWER;
                } else {
                    if (EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE == v) {
                        mShorMessageField = true;
                    }
                    int skbLayout = mInputMode & MASK_SKB_LAYOUT;
                    newInputMode = mInputMode;
                    if (0 == skbLayout) {
                        if ((mInputMode & MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                            newInputMode = MODE_SKB_CHINESE;
                        } else {
                            newInputMode = MODE_SKB_ENGLISH_LOWER;
                        }
                    }
                }
                break;
            default://保持之前的
                int skbLayout = mInputMode & MASK_SKB_LAYOUT;
                newInputMode = mInputMode;
                if (0 == skbLayout) {
                    if ((mInputMode & MASK_LANGUAGE) == MASK_LANGUAGE_CN) {
                        newInputMode = MODE_SKB_CHINESE;
                    } else {
                        newInputMode = MODE_SKB_ENGLISH_LOWER;
                    }
                }
                break;
        }
        mEditorInfo = editorInfo;
        mEditorInfo.imeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;//显式设置 横屏的时候不全屏 cpl 仅仅在这里设置不在hkbrquest中设置的话会有bug
        saveInputMode(newInputMode);
        prepareToggleStates(true);
        return  mInputIcon;
    }

    public int requestBackToPreviousSkb() {
        int layout = mInputMode & MASK_SKB_LAYOUT;
        int lastLayout = mPreviousInputMode & MASK_SKB_LAYOUT;
        if (0 != layout && 0 != lastLayout) {
            mInputMode = mPreviousInputMode;
            saveInputMode(mInputMode);
            prepareToggleStates(true);
            return  mInputIcon;
        }
        return 0;
    }

    public int getTooggleStateForCnCand() {
        return mToggleStateCnCandidates;
    }

    public boolean isEnglishWithSkb() {
        return MODE_SKB_ENGLISH_LOWER == mInputMode
                || MODE_SKB_ENGLISH_UPPER == mInputMode;
    }

    public boolean isEnglishUpperCaseWithSkb() {
        return MODE_SKB_ENGLISH_UPPER == mInputMode;
    }

    public boolean isEnglishWithHkb() {
        return MODE_HKB_ENGLISH == mInputMode;
    }

    public int getInputMode() {
        return mInputMode;
    }

    public ToggleStates getToggleStates() {
        return mToggleStates;
    }

    public boolean isChineseText() {
        int layout = mInputMode & MASK_SKB_LAYOUT;
        if (MASK_SKB_LAYOUT_QWERTY == layout || 0 == layout) { // 没有显示键盘也默认是中文s
            int language = mInputMode & MASK_LANGUAGE;
            if (language == MASK_LANGUAGE_CN) return true;
        }
        return false;
    }

    public boolean isChinieseTextWithSkb() {
        int layout = mInputMode & MASK_SKB_LAYOUT;
        if (MASK_SKB_LAYOUT_QWERTY == layout) {
            int language = mInputMode & MASK_LANGUAGE;
            if (MASK_LANGUAGE_CN == language) return true;
        }
        return false;
    }

    public boolean isChineseTextWithHkb() {
        int skbLayout = (mInputMode & MASK_SKB_LAYOUT);
        if (MODE_UNSET == skbLayout) {
            int language = (mInputMode & MASK_LANGUAGE);
            if (MASK_LANGUAGE_CN == language) return true;
        }
        return false;
    }


    public boolean isSymbolWithSkb() {
        int skbLayout = (mInputMode & MASK_SKB_LAYOUT);
        if (MASK_SKB_LAYOUT_SYMBOL1 == skbLayout
                || MASK_SKB_LAYOUT_SYMBOL2 == skbLayout) {
            return true;
        }
        return false;
    }

    public boolean isEnterNoramlState() {
        return mEnterKeyNormal;
    }

    public boolean tryHandleLongPressSwitch(int keyCode) {
        if (USERDEF_KEYCODE_LANG_2 == keyCode || USERDEF_KEYCODE_PHONE_SYM_4 == keyCode) { //普通语言切换或者数字的符号位
            mImeService.showOptionsMenu();
            return  true;
        }
        return false;
    }

    public class ToggleStates {
        public boolean mQwerty;
        public boolean mQwertyUpperCase;
        public int mRowIdToEnable;
        public int mKeyStates [] = new int [MAX_TOGGLE_STATES];//包含的键盘状态
        public int mKeyStatesNum = 0;
    }


}
