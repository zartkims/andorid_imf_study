package com.northeastern_uni.caipengli.cplinput;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.inputmethod.pinyin.IPinyinDecoderService;
import com.northeastern_uni.caipengli.cplinput.domain.KeyMapDream;
import com.northeastern_uni.caipengli.cplinput.domain.SettingHolder;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKey;
import com.northeastern_uni.caipengli.cplinput.domain.SoftKeyboardPool;
import com.northeastern_uni.caipengli.cplinput.logic.CandidatesListener;
import com.northeastern_uni.caipengli.cplinput.logic.EnglishProcessor;
import com.android.inputmethod.pinyin.PinyinDecoderService;
import com.northeastern_uni.caipengli.cplinput.utils.InputEnvironment;
import com.northeastern_uni.caipengli.cplinput.utils.MYLOG;
import com.northeastern_uni.caipengli.cplinput.utils.SoundManager;
import com.northeastern_uni.caipengli.cplinput.utils.SwitchKeyboardManager;
import com.northeastern_uni.caipengli.cplinput.view.BalloonPop;
import com.northeastern_uni.caipengli.cplinput.view.CandidatesContainer;
import com.northeastern_uni.caipengli.cplinput.view.ComposingView;
import com.northeastern_uni.caipengli.cplinput.view.FloatContainerPopup;
import com.northeastern_uni.caipengli.cplinput.view.FunctionCandidateContainer;
import com.northeastern_uni.caipengli.cplinput.view.ResizePop;
import com.northeastern_uni.caipengli.cplinput.view.SoftKeyboardContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Vector;

/**
 * Created by caipengli on 16年3月3日.
 */
public class CPLIME extends InputMethodService {
    public static final boolean SIMULATE_KEY_DELETE = true;
    public static final int MSG_SHOW_FLOAT_MODE_OVERLAY_WINDOW = 16317;
    public static final int MSG_SHOW_FLOAT_RESIZE_WINDOW = 16328;
    public static final int MSG_SHOW_FLOAT_MODE_POP_WINDOW = 16318;
    public static final int MSG_START_INPUT_READ_CONFIG = 16323;
    private static final int MSG_SWITCH_FLOAT_MODE = 1617;


    private InputEnvironment mEnvironment;

    private SwitchKeyboardManager mInputSwitch;
    private SoftKeyboardContainer mSkbContainer;
    private ComposingView mComposingView;
    /**
     * 这个是包含softkeyboardContainer和　fuctionCandidate的layout
     */
    private RelativeLayout mFuctionAndNormalCndLayout;
    private FunctionCandidateContainer mFunctionCandidateContainer;
    private CandidatesContainer mNormalCandidatesContainer;

    /**
     * 下面几个属性是和浮动窗口模式相关
     */
    private RelativeLayout mFloatRootView;
    private LinearLayout mFloatKeyboardView;//浮动的软键盘

    private RelativeLayout mFloatCandidateView;
    @Deprecated //这个是曾经的一个解决方案
    private PopupWindow mFloatModeOverlayWindow;
    private View mFloatModeOverlayView;
    private MyFloatModeHandler mFloatModeHandle = new MyFloatModeHandler();

    /**
     * 显示浮动输入法的popup
     */
    private FloatContainerPopup mFloatKeyboardPopup;

    /**
     * 这个仅仅是用来占位的不让报空
     */
    private LinearLayout mBlankLayout;

    /**
     * 这个是浮动window拖动的回调
     */
    private FloatWindowDragListener mFloatDragListener;

    /**
     * 这个是浮动模式下可调节窗口大小
     */
    private ResizePop mResizePopup;

    private RestSizeFinishListener mResizeListener;

    /**
     * 下面这三个是任何模式下的拼音组成框
     */
    private LinearLayout mFloatingComposingContainer;
    private PopupWindow mFloatingComposingWindow;
    private PopupTimer mFloatingWindowTimer = new PopupTimer();

    /**
     * 这个是功能键的popup
     */
    @Deprecated
    private BalloonPop mCandodatesBalloon;
    private ChoiceNotifier mChoiceNotifier;
    private AlertDialog mOptionsDialog;

    private DecodingInfo mDecInfo = new DecodingInfo();

    /**
     * 连接aidl后
     */
    private ServiceConnection mPinyinDecoderServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDecInfo.mIPinyinDecoderService = IPinyinDecoderService.Stub
                    .asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mDecInfo.mIPinyinDecoderService = null;
        }
    };

    private EnglishProcessor mImEn;
    private ImeState mImeState = ImeState.STATE_IDLE;

    // 如果点击的声音变了就更新下
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SoundManager.getInstance(context).updateRingerMode();
        }
    };

    public enum ImeState {
        STATE_BYPASS,
        /**
         * 空闲
         */
        STATE_IDLE,
        /**
         * 输入
         */
        STATE_INPUT,
        /**
         * 拼写输入中
         */
        STATE_COMPOSING,
        STATE_PREDICT,
        /**
         * 来源于应用本身的处理
         */
        STATE_APP_COMPLETION
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mEnvironment = InputEnvironment.getInstance();
        //开始绑定远程服务
        startPinyinDecoderService();
        mImEn = new EnglishProcessor();
        SettingHolder.getInstance(
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        mInputSwitch = new SwitchKeyboardManager(this);
        mChoiceNotifier = new ChoiceNotifier(this);
        mEnvironment.onConfigurationChanged(getResources().getConfiguration(), this);
        MYLOG.LOGI("oncreate success !");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mPinyinDecoderServiceConnection);
        SettingHolder.releaseInstance();
        mDecInfo.mIPinyinDecoderService = null;
        mDecInfo = null;
    }

    @Override
    public View onCreateInputView() {
        mSkbContainer = (SoftKeyboardContainer)
                getLayoutInflater().inflate(R.layout.softkeyboard_container, null);
        mSkbContainer.setService(this);
        mSkbContainer.setSwitchManager(mInputSwitch);
        MYLOG.LOGI("onCreateInputView success !");
        return mSkbContainer;
    }

    @Override
    public View onCreateCandidatesView() {
        LayoutInflater inflater = getLayoutInflater();
        mFloatingComposingContainer = (LinearLayout) inflater.inflate(R.layout.floating_composing_container, null);
        mComposingView = (ComposingView) mFloatingComposingContainer.getChildAt(0);//里面就一个child

        if (null != mFloatingComposingWindow && mFloatingComposingWindow.isShowing()) {
            mFloatingWindowTimer.cancelShowing();
            mFloatingComposingWindow.dismiss();
        }
        mFloatingComposingWindow = new PopupWindow(this);
        mFloatingComposingWindow.setClippingEnabled(false);//不允许超出屏幕
        mFloatingComposingWindow.setBackgroundDrawable(null);
        mFloatingComposingWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mFloatingComposingWindow.setContentView(mFloatingComposingContainer);

        //candidate vie
        mFuctionAndNormalCndLayout = (RelativeLayout) getLayoutInflater()
                .inflate(R.layout.candidate_container_both_normal_funcnd, null);
        mFunctionCandidateContainer = (FunctionCandidateContainer) mFuctionAndNormalCndLayout
                .findViewById(R.id.layout_function_candidate);
        mFloatDragListener = new FloatWindowDragListenerImpl();
        mResizeListener = new resetSizeFinishListenerImpl();
        mFunctionCandidateContainer.setDragListener(mFloatDragListener);
        mFunctionCandidateContainer.setNotifyContainer(mChoiceNotifier);
//        mNormalCandidatesContainer = (CandidatesContainer) inflater.inflate(R.layout.candidates_container, null);
        mNormalCandidatesContainer
                = (CandidatesContainer) mFuctionAndNormalCndLayout.findViewById(R.id.candidates_container);
        mNormalCandidatesContainer.initialize(mChoiceNotifier);
        MYLOG.LOGI("onCreateCandidatesView success !");

        return mFuctionAndNormalCndLayout;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        updateIcon(mInputSwitch.requestInputWithHkb(attribute));
        resetToIdleState(false);
        MYLOG.LOGI("onstartinput success !");
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        //准备软键盘状态
        updateIcon(mInputSwitch.requestInputWithSkb(info));
        resetToIdleState(false);
        //正式开始根据上面的状态进行键盘的加载
        mSkbContainer.updateInputMode();
        MYLOG.LOGI("onStartInputView success !");
        setCandidatesViewShown(true);
        //判断是否是浮动窗口模式
        mEnvironment.readFloatModeInfoFromSharePre(this);
        if (mEnvironment.isFloatMode()) {
           mFloatModeHandle.sendEmptyMessage(MSG_START_INPUT_READ_CONFIG);
        }
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        MYLOG.LOGI("onFinishInputView");
        resetToIdleState(false);
        clearKeyboardStatus();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onFinishInput() {
        MYLOG.LOGI("onFinishInput");
        clearKeyboardStatus();
        resetToIdleState(false);
        super.onFinishInput();
    }

    @Override
    public void onFinishCandidatesView(boolean finishingInput) {
        resetToIdleState(false);
        super.onFinishCandidatesView(finishingInput);
    }

    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (!isFullscreenMode()) return;
        if (null == completions || completions.length <= 0) return;
        if (null == mSkbContainer || !mSkbContainer.isShown()) return;
        if (!mInputSwitch.isChineseText() ||
                ImeState.STATE_IDLE == mImeState ||
                ImeState.STATE_PREDICT == mImeState) {
            mImeState = ImeState.STATE_APP_COMPLETION;
            mDecInfo.prepareAppCompletions(completions);
            showCandidateWindow(false);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        clearKeyboardStatus();
        mEnvironment.onConfigurationChanged(newConfig, this);
        if (null != mSkbContainer) {
            mSkbContainer.dismissPopups();
        }
        /**
         * 这个方法调用后 onCreateInputView 和 onCreateCandidatesView 会在合适的地方调用
         */
        super.onConfigurationChanged(newConfig);
        resetToIdleState(false);
    }

    @Override
    public void requestHideSelf(int flags) {
        MYLOG.LOGI("now hide self");
        if (mEnvironment.isFloatMode() && null != mResizePopup && mResizePopup.isShowing()) {
            //仅仅隐藏调整大小窗口
            mResizePopup.dismiss();
            return;
        }
        clearKeyboardStatus();
        dismissCandidateWindow();
        if (null != mSkbContainer && mSkbContainer.isShown()) {
            mSkbContainer.dismissPopups();
        }
        super.requestHideSelf(flags);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 0 != event.getRepeatCount() 防止按得太快没意义
        if (processKey(event, 0 != event.getRepeatCount())) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (processKey(event, true)) return true;
        return super.onKeyUp(keyCode, event);
    }

    /**
     *
     * @param event
     * @param realAction
     * @return
     */
    private boolean processKey(KeyEvent event, boolean realAction) {
        if (mImeState.STATE_BYPASS == mImeState) return true;
        int keyCode = event.getKeyCode();
        //如果是物理键盘的话可用 shift + 空格 来切换输入法
        if (KeyEvent.KEYCODE_SPACE == keyCode && event.isShiftPressed()) {
            if (!realAction) {
                return true;
            }
            updateIcon(mInputSwitch.switchLanguageWithHkb());
            resetToIdleState(false);
            //各种额外状态
            int allMetaState = KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON
                    | KeyEvent.META_ALT_RIGHT_ON | KeyEvent.META_SHIFT_ON
                    | KeyEvent.META_SHIFT_LEFT_ON
                    | KeyEvent.META_SHIFT_RIGHT_ON | KeyEvent.META_SYM_ON;
            getCurrentInputConnection().clearMetaKeyStates(allMetaState);//清空所有状态
            return true;
        }
        //如果是物理键盘输入英语状态,我们就不做处理交给 默认的listener去处理他
        if (mInputSwitch.isEnglishWithHkb()) {
            return false;
        }
        /**
         * 如果是功能键的话直接处理
         */
        if (processFunctionKeys(keyCode, realAction, event.getAction() == KeyEvent.ACTION_UP)) {
            return true;
        }

        int keyChar = 0;
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            keyChar = keyCode - KeyEvent.KEYCODE_A + 'a';
        } else if (keyCode >= KeyEvent.KEYCODE_0
                && keyCode <= KeyEvent.KEYCODE_9) {
            keyChar = keyCode - KeyEvent.KEYCODE_0 + '0';
        } else if (keyCode == KeyEvent.KEYCODE_COMMA) {
            keyChar = ',';
        } else if (keyCode == KeyEvent.KEYCODE_PERIOD) {
            keyChar = '.';
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            keyChar = ' ';
        } else if (keyCode == KeyEvent.KEYCODE_APOSTROPHE) {
            keyChar = '\'';
        }

        if (mInputSwitch.isEnglishWithSkb()) {
            return mImEn.processKey(getCurrentInputConnection(), event,
                    mInputSwitch.isEnglishUpperCaseWithSkb(), realAction);
        } else if (mInputSwitch.isChineseText()) {
            //以下根据不同的状态调用不同的词库方法
            if (ImeState.STATE_IDLE == mImeState ||
                    ImeState.STATE_APP_COMPLETION == mImeState){
                mImeState = ImeState.STATE_IDLE;
                return processStateIdle(keyChar, keyCode, event, realAction);
            } else if (ImeState.STATE_INPUT == mImeState) {
                return processStateInput(keyChar, keyCode, event, realAction);
            } else if (ImeState.STATE_COMPOSING == mImeState) {
                return processStateEditComposing(keyChar, keyCode, event, realAction);
            } else if (ImeState.STATE_PREDICT == mImeState) {
                return processStatePredict(keyChar, keyCode, event, realAction);
            }
        } else {
            if (0 != keyChar && realAction) {
                commitResultText(String.valueOf((char) keyChar));
            }
        }
        return false;
    }

    /**
     * 点的是功能键
     * @param keyCode
     * @param realAction
     * @return
     */
    private boolean processFunctionKeys(int keyCode, boolean realAction, boolean isAcitonUp) {
        //点击的是返回键　先隐藏所有的popup
        if (KeyEvent.KEYCODE_BACK == keyCode || KeyEvent.KEYCODE_HOME == keyCode) {
            if (mEnvironment.isFloatMode() && null != mResizePopup && mResizePopup.isShowing() && isAcitonUp) {
                //仅仅隐藏调整大小窗口
                mResizePopup.dismiss();
                return true;
            }
            //还会额外把popup给隐藏了
            if (isAcitonUp) clearKeyboardStatus();

            if (isInputViewShown()) {
                if (mSkbContainer.handleBack(realAction)) {
                    return true;
                }
            }
        }

        //中文输入的功能键将额外处理
        if (mInputSwitch.isChineseText()) {
            return false;
        }

        if (null != mNormalCandidatesContainer && mNormalCandidatesContainer.isShown()
                && !mDecInfo.isCandidatesListEmpty()) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (!realAction) return true;
                chooseCandidate(-1);
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (!realAction) return true;
                mNormalCandidatesContainer.activeCurseBackward();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (!realAction) return true;
                mNormalCandidatesContainer.activeCurseForward();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (!realAction) return true;
                mNormalCandidatesContainer.pageBackward();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (!realAction) return true;
                mNormalCandidatesContainer.pageForward();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DEL
                    && ImeState.STATE_PREDICT == mImeState) {
                if (!realAction) return true;
                resetToIdleState(false);
                return true;
            }
        } else { //candidate　是空的
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (!realAction) return true;
                if (SIMULATE_KEY_DELETE) { //　模拟delete
                    simulateKeyEventDownUp(keyCode);
                } else {
                    getCurrentInputConnection().deleteSurroundingText(1, 0);
                }
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (!realAction) return true;
                sendKeyChar('\n');
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_SPACE) {
                if (!realAction) return true;
                sendKeyChar(' ');
                return true;
            }
        }
        return false;
    }

    /**
     * 完成或者闲置的时候
     * @param keyChar
     * @param keyCode
     * @param event
     * @param realAction
     * @return
     */
    private boolean processStateIdle(int keyChar, int keyCode, KeyEvent event, boolean realAction) {
        //如果是a~z直接转化为输入状态
        if ('a' <= keyChar && keyChar <= 'z' && !event.isAltPressed()) {
            if (!realAction) return true;
            mDecInfo.addSplChar((char) keyChar, true);
            chooseAndUpdate(-1);
            return true;
        } else if (KeyEvent.KEYCODE_DEL == keyCode) {
            if (!realAction) return true;
            if (SIMULATE_KEY_DELETE) {
                simulateKeyEventDownUp(keyCode);
            } else {
                getCurrentInputConnection().deleteSurroundingText(1, 0);
            }
            return true;
        } else if (KeyEvent.KEYCODE_ENTER == keyCode) {
            if (!realAction) return true;
            sendKeyChar('\n');
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ALT_LEFT
                || keyCode == KeyEvent.KEYCODE_ALT_RIGHT
                || keyCode == KeyEvent.KEYCODE_SHIFT_LEFT
                || keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
            return true;
        } else if (event.isAltPressed()) { //alt模式下
            char fullWidthChar = KeyMapDream.getChineseLabel(keyCode);
            if (0 != fullWidthChar) {
                if (realAction) {
                    String result = String.valueOf(fullWidthChar);
                    commitResultText(result);
                    return true;
                }
            } else {
                if (keyCode >= KeyEvent.KEYCODE_A || KeyEvent.KEYCODE_Z >= keyCode) {
                    return true;
                }
            }
        } else if (keyChar != 0 && keyChar != '\t') {
            if (realAction) {
                if (',' == keyChar || '.' == keyChar) {
                    inputCommaPeriod("", keyChar, false, ImeState.STATE_IDLE);
                } else {
                    commitResultText(String.valueOf((char)keyChar));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 正常输入状态
     * @param keyChar
     * @param keyCode
     * @param event
     * @param realAction
     * @return
     */
    private boolean processStateInput(int keyChar, int keyCode, KeyEvent event, boolean realAction) {
        //
        if (event.isAltPressed()) {
            if ('\'' != event.getUnicodeChar(event.getMetaState())) {
                if (!realAction) return true;
                char fullWidthChar = KeyMapDream.getChineseLabel(keyCode);
                if (0 != fullWidthChar) {
                    commitResultText(mDecInfo
                            .getCurrentFullSent(mNormalCandidatesContainer.getActiveCandiatePos())
                            + String.valueOf(fullWidthChar));
                    resetToIdleState(false);
                }
                return true;
            } else {
                keyChar = '\'';
            }
        }

        if ('a' <= keyChar && keyChar <= 'z' || keyChar == '\''
                && !mDecInfo.charBeforeCursorIsSeparator()
                || keyCode == KeyEvent.KEYCODE_DEL) { //正常的按键输入
            if (!realAction) return true;
            return processSurfaceChange(keyChar, keyCode);
        } else if (',' == keyChar || '.' == keyChar) {
            inputCommaPeriod(mDecInfo.getCurrentFullSent(mNormalCandidatesContainer.getActiveCandiatePos()),
                    keyChar, true, ImeState.STATE_IDLE);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!realAction) return true;
            mNormalCandidatesContainer.activeCurseBackward();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!realAction) return true;
            mNormalCandidatesContainer.activeCurseForward();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (!realAction) return true;
            mNormalCandidatesContainer.pageBackward();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!realAction) return true;
            mNormalCandidatesContainer.pageForward();
            return true;
        } else if (KeyEvent.KEYCODE_1 <= keyChar && keyCode <= KeyEvent.KEYCODE_9) {
            //不可通过硬键盘 1 ~ 9 来选择候选词 因为现在是滑动的的所以没有第几个这个概念
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!realAction) return true;
            if (mInputSwitch.isEnterNoramlState()) {
                commitResultText(mDecInfo.getOriginalSplStr().toString());
                resetToIdleState(false);
            } else {
                commitResultText(mDecInfo
                        .getCurrentFullSent(mNormalCandidatesContainer.getActiveCandiatePos()));
                sendKeyChar('\n');
                resetToIdleState(false);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (!realAction) return true;
            chooseCandidate(-1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!realAction) return true;
            resetToIdleState(false);
            requestHideSelf(0);
            return true;
        }
        return false;
    }

    /**
     * 显然这是选完后预测状态
     * @param keyChar
     * @param keyCode
     * @param event
     * @param realAction
     * @return
     */
    private boolean processStatePredict(int keyChar, int keyCode, KeyEvent event, boolean realAction) {
        if (!realAction) return true;
        if (event.isAltPressed()) {
            char fullChar = KeyMapDream.getChineseLabel(keyCode);
//            MYLOG.LOGI("my ori fullchar" + fullChar);
            if (0 != fullChar) {
                commitResultText(mDecInfo.getCandidate(mNormalCandidatesContainer.getActiveCandiatePos())
                + String.valueOf(fullChar));//加上对应的转码
                resetToIdleState(false);
            }
            return true;
        }

        if ('a' <= keyChar && keyChar <= 'z') {//说明用户又重新输入了
            mDecInfo.addSplChar((char) keyChar, true);
            changeToStateInput(true);
            chooseAndUpdate(-1);
        } else if (',' == keyChar || '.' == keyChar) {
            inputCommaPeriod("", keyChar, true, ImeState.STATE_IDLE);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            mNormalCandidatesContainer.activeCurseBackward();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mNormalCandidatesContainer.activeCurseForward();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            mNormalCandidatesContainer.pageBackward();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            mNormalCandidatesContainer.pageForward();
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            resetToIdleState(false);
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false);
            requestHideSelf(0);
        } else if (KeyEvent.KEYCODE_1 <= keyCode && keyCode <= KeyEvent.KEYCODE_9) {
        } else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            sendKeyChar('\n');
            resetToIdleState(false);
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_SPACE) { //如果是空格或者是回车的话默认就是选择当前的词
            chooseCandidate(-1);
        }
        return true;
    }

    /**
     * 在编辑修改拼音这个小操作
     * @param keyChar
     * @param keyCode
     * @param event
     * @param realAction
     * @return
     */
    private boolean processStateEditComposing(int keyChar, int keyCode, KeyEvent event, boolean realAction) {
        if (!realAction) return true;
        ComposingView.ComposingStatus composStatus = mComposingView.getComposingStatus();
        //ALT键在拼音输入法中如果代表的是＇的话　用作的是分词
        if (event.isAltPressed()) {
            if ('\'' != event.getUnicodeChar(event.getMetaState())) { //如果不是分词符
                char fullwidth_char = KeyMapDream.getChineseLabel(keyCode);
                if (0 != fullwidth_char) {
                    String resStr;
                    if (ComposingView.ComposingStatus.SHOW_STRING_LOWERCASE == composStatus) {
                        resStr = mDecInfo.getOriginalSplStr().toString();
                    } else {
                        resStr = mDecInfo.getComposingStr();
                    }
                    commitResultText(resStr + String.valueOf(fullwidth_char));
                    resetToIdleState(false);
                }
                return true;
            } else {
                keyChar = '\'';
            }
        }
        //这种上下左右的基本都是为了兼顾老式的物理键盘
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) { //方向键向下
            if (!mDecInfo.isSelectionFinished()) {
                changeToStateInput(true);
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            mComposingView.moveCursor(keyCode);
        } else if ((keyCode == KeyEvent.KEYCODE_ENTER && mInputSwitch.isEnterNoramlState())
                || keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_SPACE) { //这个是回车或者确定
            if (ComposingView.ComposingStatus.SHOW_STRING_LOWERCASE == composStatus) {
                String str = mDecInfo.getOriginalSplStr().toString();
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str);
                }
            } else if (ComposingView.ComposingStatus.EDIT_PINYIN == composStatus) {
                String str = mDecInfo.getComposingStr();
                if (!tryInputRawUnicode(str)) {
                    commitResultText(str);
                }
            } else {//普通正常状态
                commitResultText(mDecInfo.getComposingStr());
            }
            resetToIdleState(false);
        } else if (keyChar == KeyEvent.KEYCODE_ENTER && !mInputSwitch.isEnterNoramlState()) {
            //当enter键不是单纯的回车的时候
            String restStr;
            if (!mDecInfo.isCandidatesListEmpty()) {
                restStr = mDecInfo.getCurrentFullSent(mNormalCandidatesContainer.getActiveCandiatePos());
            } else { //候选词为空的时候
                restStr = mDecInfo.getComposingStr();
            }
            commitResultText(restStr);
            sendKeyChar('\n');//多给个回车
            resetToIdleState(false);
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            resetToIdleState(false);
            requestHideSelf(0);
            return true;
        } else {
            //说明按下的不是特殊键 而是普通的字母键 那么的话就重置解析
            return processSurfaceChange(keyChar, keyCode);
        }
        return true;
    }

    private boolean processSurfaceChange(int keyChar, int keyCode) {
        //如果到达可输入的最大解析长度而且点击的不是回车的话就直接忽略
        if (mDecInfo.isSplStrFull() && KeyEvent.KEYCODE_DEL != keyCode) {
            return true;
        }
        if (('a' <= keyChar && keyChar <= 'z')
                || (keyChar == '\'' && !mDecInfo.charBeforeCursorIsSeparator()) //是分词符 而且分词符不可连续
                || ((('0' <= keyChar && keyChar <= '9') || keyChar == ' ') && ImeState.STATE_COMPOSING == mImeState)) {
            mDecInfo.addSplChar((char) keyChar, false);
            chooseAndUpdate(-1);
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            mDecInfo.prepareDeleteBeforeCursor();
            chooseAndUpdate(-1);
        }
        return true;
    }

    /**
     *将逗号和句号转义以下再法不然可能会出错
     *
     * @param preEdit
     * @param keyChar
     * @param dismissCandWindow
     * @param nextState
     */
    private void inputCommaPeriod(String preEdit, int keyChar, boolean dismissCandWindow, ImeState nextState) {
        if (keyChar == ',') {
            preEdit += '\uff0c';
        } else if (keyChar == '.') {
            preEdit += '\u3002';
        } else {
            return;
        }
        commitResultText(preEdit);
        if (dismissCandWindow) resetCandidateWindow();
        mImeState = nextState;
    }

    private void chooseCandidate(int activeCandNo) {
        if (activeCandNo < 0) {
            activeCandNo = mNormalCandidatesContainer.getActiveCandiatePos();
        }
        if (activeCandNo >= 0) {
            chooseAndUpdate(activeCandNo);
        }
    }

    /**
     * 对开头或皆为为unicode的词进行转化然后提交
     * @param str
     * @return
     */
    private boolean tryInputRawUnicode(String str) {
        if (str.length() > 7) {
            if (str.substring(0, 7).compareTo("unicode") == 0) {
                try {
                    String digitStr = str.substring(7);
                    int startPos = 0;
                    int radix = 10;
                    if (digitStr.length() > 2 && digitStr.charAt(0) == '0'
                            && digitStr.charAt(1) == 'x') {
                        startPos = 2;
                        radix = 16;
                    }
                    digitStr = digitStr.substring(startPos);
                    int unicode = Integer.parseInt(digitStr, radix);
                    if (unicode > 0) {
                        char low = (char) (unicode & 0x0000ffff);
                        char high = (char) ((unicode & 0xffff0000) >> 16);
                        commitResultText(String.valueOf(low));
                        if (0 != high) {
                            commitResultText(String.valueOf(high));
                        }
                    }
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            } else if (str.substring(str.length() - 7, str.length()).compareTo(
                    "unicode") == 0) {
                String resultStr = "";
                for (int pos = 0; pos < str.length() - 7; pos++) {
                    if (pos > 0) {
                        resultStr += " ";
                    }

                    resultStr += "0x" + Integer.toHexString(str.charAt(pos));
                }
                commitResultText(String.valueOf(resultStr));
                return true;
            }
        }
        return false;
    }

    /**
     * @param candId 这个小于 0 的时候将会重新解析
     */
    private void chooseAndUpdate(int candId) {
        if (!mInputSwitch.isChineseText()) {
            String choice = mDecInfo.getCandidate(candId);
            if (null != choice) {
                commitResultText(choice);
            }
            resetToIdleState(false);
            return;
        }
        //根据不同的状态取加载词库
        if (ImeState.STATE_PREDICT != mImeState) {
            mDecInfo.chooseDecodingCandidate(candId);
        } else {
            mDecInfo.choosePredictChoice(candId);
        }

        if (mDecInfo.getComposingStr().length() > 0) {
            String result = mDecInfo.getComposingStrActivePart();
            if (candId >= 0 && mDecInfo.canDoPrediction()) {//逻辑上能预测
                commitResultText(result);
                mImeState = ImeState.STATE_PREDICT;
                if (null != mSkbContainer && mSkbContainer.isShown()) {
                    mSkbContainer.toggleCandidateMode(false);
                }
                if (SettingHolder.getPrediction()) {//设置上能预测
                    InputConnection ic = getCurrentInputConnection();
                    if (null != ic) {
                        CharSequence cs = ic.getTextBeforeCursor(3, 0);//取得输入框的内容用来预测
                        if (null != cs) {
                            mDecInfo.preparePredicts(cs);
                        }
                    }
                } else {//设置上不能预测
                    mDecInfo.resetCandidates();
                }

                if (mDecInfo.mCandidatesList.size() > 0) {
                    showCandidateWindow(false);
                } else {
                    resetToIdleState(false);
                }
            } else {
                if (ImeState.STATE_IDLE == mImeState) {
                    if (0 == mDecInfo.getSplStrDecodedLen()) {
                        changeToStateComposing(true);
                    } else {
                        changeToStateInput(true);
                    }
                } else {
                    if (mDecInfo.isSelectionFinished()) {
                        changeToStateComposing(true);
                    }
                }
                showCandidateWindow(true);
            }
        } else {
            resetToIdleState(false);
        }
    }

    /**
     * 这个是
     * @param keyCode
     */
    private void simulateKeyEventDownUp(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (null == ic) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    private boolean startPinyinDecoderService() {
        if (null == mDecInfo.mIPinyinDecoderService) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClass(this, PinyinDecoderService.class);
            if (bindService(serviceIntent,
                    mPinyinDecoderServiceConnection,  Context.BIND_AUTO_CREATE)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void updateIcon(int iconId) {
        if (iconId > 0) {
            showStatusIcon(iconId);
        } else {
            hideStatusIcon();
        }
    }

    /**
     *
     * @param resetInlineText
     */
    private void resetToIdleState(boolean resetInlineText) {
        if (ImeState.STATE_IDLE == mImeState) {
            return;
        }
        mImeState = ImeState.STATE_IDLE;
        mDecInfo.reset();
        if (null != mComposingView) {
            mComposingView.reset();
        }
        setShowFunctionCandidate(true);
        if (resetInlineText) {
            commitResultText("");
        }
        resetCandidateWindow();
    }

    private void changeToStateInput(boolean updateUI) {
        mImeState = ImeState.STATE_INPUT;
        if (!updateUI) return;
        if (null != mSkbContainer && mSkbContainer.isShown()) {
            mSkbContainer.toggleCandidateMode(true);
        }
        showCandidateWindow(true);
    }

    private void changeToStateComposing(boolean updateUi) {
        mImeState = ImeState.STATE_COMPOSING;
        if (!updateUi) return;

        if (null != mSkbContainer && mSkbContainer.isShown()) {
            mSkbContainer.toggleCandidateMode(true);
        }
    }

    private void dismissCandidateWindow() {
        if (null == mNormalCandidatesContainer) return;
        try {
            mFloatingWindowTimer.cancelShowing();
            mFloatingComposingWindow.dismiss();
        } catch (Exception e) {}
        setCandidatesViewShown(false);
        if (null != mSkbContainer && mSkbContainer.isShown()) {
            mSkbContainer.toggleCandidateMode(false);
        }
    }

    private void resetCandidateWindow() {
        if(null == mNormalCandidatesContainer) return;
        //候选词的消失
        try {
            mFloatingWindowTimer.cancelShowing();
            mFloatingComposingWindow.dismiss();
        } catch (Exception e){
            MYLOG.LOGE("fail to dismiss" + e);
        }
        if (null != mSkbContainer && mSkbContainer.isShown()) {
            mSkbContainer.toggleCandidateMode(false);
        }
        mDecInfo.resetCandidates();
        if (null != mNormalCandidatesContainer && mNormalCandidatesContainer.isShown()) {
            showCandidateWindow(false);
            return;
        }
    }

    private void showCandidateWindow(boolean showComposingView) {
        setShowFunctionCandidate(false);
        setCandidatesViewShown(true);
        MYLOG.LOGI("success show candidate view setCandidatesViewShown");
        if (null != mSkbContainer) {
            mSkbContainer.requestLayout();
        }
        if (null == mNormalCandidatesContainer) {
            resetToIdleState(false);
            return;
        }
        updateComposingText(showComposingView);
        mNormalCandidatesContainer.showCandidates(mDecInfo);
        mFloatingWindowTimer.postShowFloatWindow();
    }

    private void commitResultText(String resultText) {
        InputConnection ic = getCurrentInputConnection();
        if (null != ic) {
            ic.commitText(resultText, 1);
        }
        if (null != mComposingView) {//提交了之后候组词框消失
            mComposingView.setVisibility(View.INVISIBLE);
            mComposingView.invalidate();
        }
    }

    private void updateComposingText(boolean visible) {
        if (!visible) {
            mComposingView.setVisibility(View.INVISIBLE);
        } else {
            mComposingView.setDecodingInfo(mDecInfo, mImeState);
            mComposingView.setVisibility(View.VISIBLE);
        }
        mComposingView.invalidate();
    }


    /**
     * 强行将选中的softkey进行包装成 keyEvent
     * 然后触发其他事件处理
     * @param softKey
     */
    public void responseSoftKeyEvent(SoftKey softKey) {
        if (null == softKey) return;
//        MYLOG.LOGI("ime response key : " + softKey.getKeyLabel());
        InputConnection ic = getCurrentInputConnection();
        if (null == ic) return;

        int keyCode = softKey.getKeyCode();
        //如果是通用都键
        if (softKey.isKeyCodeKey()) {
            if (processFunctionKeys(keyCode, true, true)) return;
        }

        if (softKey.isUserDefKey()) {
            updateIcon(mInputSwitch.switchModeForUserKey(keyCode));
            resetToIdleState(false);
            mSkbContainer.updateInputMode();
        } else {
            if (softKey.isKeyCodeKey()) {
                KeyEvent eDown = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                        keyCode, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD);
                KeyEvent eUp = new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode,
                        0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD);
                onKeyDown(keyCode, eDown);
                onKeyUp(keyCode, eUp);
            } else if (softKey.isUniStrKey()) { //如果是直接是unicode的话 合适的话就直接上屏吧
                boolean kUsed = false;
                String keyLabel = softKey.getKeyLabel();

                if (mInputSwitch.isChinieseTextWithSkb()
                        && (ImeState.STATE_INPUT == mImeState || ImeState.STATE_COMPOSING == mImeState)) {
                    //如果是中文的分词符
                    if (mDecInfo.length() > 0 && keyLabel.length() == 1
                            && keyLabel.charAt(0) == '\'') {
                        processSurfaceChange('\'', 0);
                        kUsed = true;
                    }
                }
                //如果不是分词符
                if (!kUsed) {
                    if (ImeState.STATE_INPUT == mImeState) {
                        commitResultText(mDecInfo
                                .getCurrentFullSent(mNormalCandidatesContainer.getActiveCandiatePos()));
                    } else if (ImeState.STATE_COMPOSING == mImeState) {
                        commitResultText(mDecInfo.getComposingStr());
                    }
                    commitResultText(keyLabel);
                    resetToIdleState(false);
                }
            }

            if (!mSkbContainer.isCurrentSkbSticky()) {
                updateIcon(mInputSwitch.requestBackToPreviousSkb());
                resetToIdleState(false);
                mSkbContainer.updateInputMode();
            }
        }
    }

    /**
     * 当选择了某个具体的候选词的时候会回调这个方法s
     * @param choiced
     */
    private void onChoiceTouch(int choiced) {
        if (ImeState.STATE_COMPOSING == mImeState) {
            changeToStateInput(true);
        } else if (mImeState.STATE_INPUT == mImeState || mImeState.STATE_PREDICT == mImeState) {
            chooseCandidate(choiced);
        } else if (mImeState == ImeState.STATE_APP_COMPLETION) {
            if (null != mDecInfo.mAppcompletions
                    && choiced > 0 && choiced < mDecInfo.mAppcompletions.length) {
                CompletionInfo completionInfo = mDecInfo.mAppcompletions[choiced];
                if (null != completionInfo) {
                    getCurrentInputConnection().commitCompletion(completionInfo);
                }
            }
            resetToIdleState(false);
        }
    }

    public void setShowFunctionCandidate(boolean isShow) {
        if (isShow) {
            mFunctionCandidateContainer.setVisibility(View.VISIBLE);
            mNormalCandidatesContainer.setVisibility(View.INVISIBLE);
        } else {
            mFunctionCandidateContainer.setVisibility(View.INVISIBLE);
            mNormalCandidatesContainer.setVisibility(View.VISIBLE);
        }
        mFuctionAndNormalCndLayout.invalidate();
    }

    /**
     * 长按了切换键时弹出菜单
     */
    public void showOptionsMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, null);
        CharSequence itemSettings = getString(R.string.setting);
        CharSequence switchInput = getString(R.string.switch_input_method_change);
        CharSequence floatModel = getString(R.string.float_model);
        builder.setItems(new CharSequence[] {itemSettings, switchInput, floatModel},
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface di, int position) {
                        di.dismiss();
                        switch (position) {
                            case 0:
                                launchSettings();
                                break;
                            case 1:
                                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                imm.showInputMethodPicker();//cpl
//                               InputMethodManager.getInstance()
//                                       .showInputMethodPicker();
                                break;
                            case 2:
                                mFloatModeHandle.sendEmptyMessageDelayed(MSG_SWITCH_FLOAT_MODE, 20);
                                Toast.makeText(CPLIME.this, "show the float modle", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
        builder.setTitle(getString(R.string.app_name));
        mOptionsDialog = builder.create();
        Window window = mOptionsDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = mSkbContainer.getWindowToken();
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mOptionsDialog.show();
    }

    private void launchSettings() {
        Intent intent = new Intent();
        intent.setClass(CPLIME.this, InputSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class PopupTimer extends Handler implements Runnable {
        private int mParentLocation [] = new int[2];

        public void postShowFloatWindow() {
            mFloatingComposingContainer.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            mFloatingComposingWindow.setWidth(mFloatingComposingContainer.getMeasuredWidth());
            mFloatingComposingWindow.setHeight(mFloatingComposingContainer.getMeasuredHeight());
            post(this);
        }

        public void cancelShowing() {
            if (mFloatingComposingWindow.isShowing()) {
                mFloatingComposingWindow.dismiss();
            }
            removeCallbacks(this);
        }
        @Override
        public void run() {
            try {
                if (!mEnvironment.isFloatMode()) {
                    mFuctionAndNormalCndLayout.getLocationInWindow(mParentLocation);
                    if (!mFloatingComposingWindow.isShowing()) {
                        mFloatingComposingWindow.showAtLocation(mFuctionAndNormalCndLayout, Gravity.LEFT | Gravity.TOP,
                                mParentLocation[0], mParentLocation[1] - mFloatingComposingWindow.getHeight());
                    } else {
                        mFloatingComposingWindow
                                .update(mParentLocation[0], mParentLocation[1] - mFloatingComposingWindow.getHeight(),
                                        mFloatingComposingWindow.getWidth(), mFloatingComposingWindow.getHeight());
                    }
                } else { //由于浮动窗口状态下candidateView被添加到了popup中所以要额外处理
                    mBlankLayout.getLocationInWindow(mParentLocation);
                    int temp [] = new int [2];
                    mFuctionAndNormalCndLayout.getLocationOnScreen(temp);
                    int gap = mEnvironment.getDisplayMetrics(getApplicationContext()).heightPixels - temp[1];
                    if (!mFloatingComposingWindow.isShowing()) {
                        //MYLOG.LOGI("the show temp x_y : " + temp[0] + "_" + temp[1]);
                        mFloatingComposingWindow.showAtLocation(mBlankLayout, Gravity.TOP | Gravity.LEFT,
                                temp[0], (mParentLocation[1] - mFloatingComposingWindow.getHeight()) - gap);
                    } else {
                        mFloatingComposingWindow
                                .update(temp[0], (mParentLocation[1] - mFloatingComposingWindow.getHeight()) - gap,
                                        mFloatingComposingWindow.getWidth(), mFloatingComposingWindow.getHeight());
                    }
                }

            } catch (Exception e) {
                MYLOG.LOGE("show composing error " + e.toString());
            }
        }
    }

    /**
     * 主要是就是candidateListener来监听候选词的选择等操作
     */
    private class ChoiceNotifier extends Handler implements CandidatesListener {
        private CPLIME mIme;

        ChoiceNotifier (CPLIME ime) {
            this.mIme = ime;
        }

        @Override
        public void onClickChoice(int choiced) {
            if (choiced >= 0) {
                mIme.onChoiceTouch(choiced);
            }
        }

        @Override
        public void onToLeftGesture() {
            if (ImeState.STATE_COMPOSING == mImeState) {
                changeToStateInput(true);
            }
            mNormalCandidatesContainer.pageForward();
        }

        @Override
        public void onToRightGesture() {
            if (ImeState.STATE_COMPOSING == mImeState) {
                changeToStateInput(true);
            }
            mNormalCandidatesContainer.pageBackward();
        }

        @Override
        public void onSwitchFloatMode() {
            mFloatModeHandle.sendEmptyMessageDelayed(MSG_SWITCH_FLOAT_MODE, 20);
        }

        @Override
        public void hideWindow() {
            requestHideSelf(0);
        }

        @Override
        public void requestSetting() {
            launchSettings();
        }

        @Override
        public void showResizePopup() {
            mFloatModeHandle.sendEmptyMessage(MSG_SHOW_FLOAT_RESIZE_WINDOW);
        }
    }

    /***
     * 这个就是连接aidl进行词库解析的关键类了
     */
    public class DecodingInfo extends Observable {
        /**最大拼音长度*/
        private static final int PY_STRING_MAX = 90;

        private StringBuffer mSurface;//用于存pinyin

        /**
         * 这个是用于native传参用
         */
        private byte [] mPyBuff;

        /**
         * 这个是成功被engin解析的长度
         */
        private int mSurfaceDecodedLen;

        /**
         * 从词库中直接加载出来的
         */
        private String mComposingStr;
        private int mActiveComsLen;

        /**实际展示的内容由mComposingStr复制而来, 不过适当添加了空格*/
        private String mComposingStrDisplay;
        private int mActiveCmpsDisplayLen;

        //第一个候选词 也是唯一的一个完整的句子
        private String mFullSent;
        //已经选定的词
        private int mFixedLen;
        //用于判断输入是否结束
        private boolean mFinishSelection;
        /**
         * 每个词的开头字母的在原来所用字母中的位置
         * 其中第一个是用来保存一共又多少个词的 可以理解为数组size
         * 这个是通过解析后的 比如 zhichi 显然[2]中保存的就是c的位置
         */
        private int mSplStart [];
        //光标位置
        private int mCursorPos;
        private IPinyinDecoderService mIPinyinDecoderService;

        private CompletionInfo [] mAppcompletions;
        //总的候选词数量
        public int mTatalChoicesNum;

        public List<String> mCandidatesList = new Vector<String>();
        /**
         * @deprecated
         */
        public Vector<Integer> mPagesStart = new Vector<Integer>();

        /**每页有多少各候选词
         * @deprecated
         * */
        public Vector<Integer> mCnToPage = new Vector<Integer>();
        //删除的字符位置
        public int mPosDelSpl = -1;
        /**
         * 当mPosDelSpl 大于等于0的时候,用来判断是表示 是在 拼写 还是在 候选词
         */
        public boolean mIsPosInSpl;

        public DecodingInfo() {
            mSurface = new StringBuffer();
            mSurfaceDecodedLen = 0;
        }

        public void reset() {
            mSurface.delete(0, mSurface.length());
            mSurfaceDecodedLen = 0;
            mCursorPos = 0;
            mFixedLen = 0;
            mFinishSelection = false;
            mComposingStr = "";
            mComposingStrDisplay = "";
            mActiveComsLen = 0;
            mActiveCmpsDisplayLen = 0;
            resetCandidates();
        }

        public void resetCandidates() {
            mCandidatesList.clear();
            mTatalChoicesNum = 0;
            candidatesChange();
        }

        public void candidatesChange() {
            candidatesChange(true);
        }
        /**
         *
         * @param isResetCandidateViewAll 如果true将会重置所有包括ＵＩ和数据　如果false仅仅重置数据ＵＩ不会被刷新
         */
        public void candidatesChange(boolean isResetCandidateViewAll) {
            setChanged();
            notifyObservers(isResetCandidateViewAll);
        }

        //// T ODO: 16年3月9日 这个函数还应该肩负起判断是否有后续的判断　以此来优化 // 已完成
        public void loadMore(boolean isResetCandidateViewAll) {
            MYLOG.LOGI("looad more");
            getCandiadtesForCache(isResetCandidateViewAll);
        }

        private void getCandiadtesForCache() {
            getCandiadtesForCache(true);
        }
        /***
         * 是否完全重置 CandidateScrollView
         * @param isResetCandidateViewAll
         */
        private void getCandiadtesForCache(boolean isResetCandidateViewAll) {
            int fetchStart = mCandidatesList.size();
            int fetchSize = InputEnvironment.LOAD_SEQUENT_SIZE;
            int residualSize = mTatalChoicesNum - fetchStart;
            fetchSize = fetchSize > residualSize ? residualSize : fetchSize;
            try {
                List<String> newList = null;
                if (ImeState.STATE_INPUT == mImeState
                        || ImeState.STATE_IDLE == mImeState
                        || ImeState.STATE_COMPOSING == mImeState) {
                    newList = mIPinyinDecoderService
                            .imGetChoiceList(fetchStart, fetchSize, mFixedLen);
                } else if (ImeState.STATE_PREDICT == mImeState) {
                    newList = mIPinyinDecoderService.imGetPredictList(fetchStart, fetchSize);
                } else if (ImeState.STATE_APP_COMPLETION == mImeState) {
                    newList = new ArrayList<String>();
                    if (null != mAppcompletions) {
                        for (int pos = fetchStart; pos < fetchSize; pos++) {
                            CompletionInfo ci = mAppcompletions[pos];
                            if (null != ci) {
                                CharSequence s = ci.getText();
                                if (null != s) newList.add(s.toString());
                            }
                        }
                    }
                }
                mCandidatesList.addAll(newList);
                candidatesChange(isResetCandidateViewAll);

                for (String s : mCandidatesList) {
                    MYLOG.LOGI("-----" + s + "-----");
                }
                MYLOG.LOGI("======" + mCandidatesList.size() + "=======");
            } catch (RemoteException re) {
                MYLOG.LOGW(re.toString());
            }
        }

        public boolean isCandidatesListEmpty() {
            return mCandidatesList.size() == 0;
        }

        public boolean isSplStrFull() {
            return mSurface.length() >= PY_STRING_MAX - 1 ;
        }

        /**
         *
         * @param ch
         * @param reset 重置所有
         */
        public void addSplChar(char ch, boolean reset) {
            if (reset) {
                mSurface.delete(0, mSurface.length());
                mSurfaceDecodedLen = 0;
                mCursorPos = 0;
                try {
                    mIPinyinDecoderService.imResetSearch();
                } catch (RemoteException e) {}
            }
            mSurface.insert(mCursorPos, ch);
            mCursorPos++;
        }

        /**
         * 标记要删除的位置
         */
        public void prepareDeleteBeforeCursor() {
            if (mCursorPos > 0) {
                int pos;
                for (pos = 0; pos < mFixedLen; pos++) {
                    if (mSplStart[pos + 2] >= mCursorPos
                            && mSplStart[pos + 1] < mCursorPos) { //mSplStart[pos + 2] == curpos
                        mPosDelSpl = pos;
                        mCursorPos = mSplStart[pos + 1];
                        mIsPosInSpl = true;
                        break;
                    }
                }
                if (mPosDelSpl < 0) {
                    mPosDelSpl = mCursorPos - 1;
                    mCursorPos--;
                    mIsPosInSpl = false;
                }
            }
        }

        public int length() {
            return mSurface.length();
        }

        public char charAt(int index) {
            return mSurface.charAt(index);
        }

        public StringBuffer getOriginalSplStr() {
            return mSurface;
        }

        public int getSplStrDecodedLen() {
            return mSurfaceDecodedLen;
        }

        public int [] getSplStart() {
            return  mSplStart;
        }

        public String getComposingStr() {
            return mComposingStr;
        }

        /**
         * 返回有效的部分那
         * @return
         */
        public String getComposingStrActivePart() {
            return mComposingStr.substring(0, mActiveComsLen);
        }

        public int getActiveCmpsLen() {
            return mActiveComsLen;
        }

        public String getComposingStrForDisplay() {
            return mComposingStrDisplay;
        }

        public int getActiveCmpsDisplayLen() {
            return mActiveCmpsDisplayLen;
        }

        public String getFullSent() {
            return mFullSent;
        }

        public String getCurrentFullSent(int activeCandPos) {
            try {
                String retStr = mFullSent.substring(0, mFixedLen);
                retStr = mCandidatesList.get(activeCandPos);
                return retStr;
            } catch (Exception e) {
                return "";
            }
        }

        public boolean candidatesFromApp() {
            return ImeState.STATE_APP_COMPLETION == mImeState;
        }

        /**
         * 是否进行预测
         */
        public boolean canDoPrediction() {
            return mComposingStr.length() == mFixedLen;
        }

        public boolean isSelectionFinished() {
            return mFinishSelection;
        }

        public void preparePredicts(CharSequence history) {
            if (null == history) return;
            resetCandidates();
            if (SettingHolder.getPrediction()) {
                String preEdit = history.toString();
                if (null != preEdit) {
                    try {
                        mTatalChoicesNum = mIPinyinDecoderService.imGetPredictsNum(preEdit);
                    } catch (RemoteException e) {
                        MYLOG.LOGE("predict error " + e.toString());
                        return;
                    }
                }
            }
            loadMore(true);
            mFinishSelection = false;
        }

        private void prepareAppCompletions(CompletionInfo [] completionInfos) {
            resetCandidates();
            mAppcompletions = completionInfos;
            mTatalChoicesNum = completionInfos.length;
            loadMore(true);
            mFinishSelection = false;
        }

        /**
         * 当用户选择了一个候选词后,engin会继续解析剩下的
         * @param candId 选择的候选词id 如果小于0说明用户没有选择任何
         */
        private void chooseDecodingCandidate(int candId) {
            if (ImeState.STATE_PREDICT != mImeState) {
                resetCandidates();//重置全部的候选词,因为要重新解析
                int totalChoicesNum = 0;
                try {
                    if (candId < 0) {
                        if (length() == 0) {//没有任何的输入
                            totalChoicesNum = 0;
                        } else {
                            if (mPyBuff == null) {
                                mPyBuff = new byte[PY_STRING_MAX];
                            }
                            for (int i = 0; i < length(); i++) {
                                mPyBuff[i] = (byte) charAt(i);//这个都是操作stringBuff
                            }

                            mPyBuff[length()] = 0;

                            if (mPosDelSpl < 0) { //当啥也没有的时候
                                totalChoicesNum
                                        = mIPinyinDecoderService.imSearch(mPyBuff, length());
                                //MYLOG.LOGI("imSearch: " + totalChoicesNum);
                            } else {
                                boolean clear_fixed_this_step = true;
                                if (ImeState.STATE_COMPOSING == mImeState) {
                                    clear_fixed_this_step = false;
                                }
                                totalChoicesNum = mIPinyinDecoderService
                                            .imDelSearch(mPosDelSpl, mIsPosInSpl,
                                                    clear_fixed_this_step);
                                mPosDelSpl = -1;
                            }
                        }
                    } else { //已经有了词然后又选了
                        totalChoicesNum = mIPinyinDecoderService.imChoose(candId);
                    }
                } catch (RemoteException re){
                    MYLOG.LOGE(re.toString());
                }

                updateDecInfoForSearch(totalChoicesNum);
            }
        }

        private void updateDecInfoForSearch(int totalChoicesNum) {
            mTatalChoicesNum = totalChoicesNum;
            if (mTatalChoicesNum < 0) {
                mTatalChoicesNum = 0;
                return;
            }
            try {
                String pyStr;
                mSplStart = mIPinyinDecoderService.imGetSplStart();
//                MYLOG.LOGI(mSplStart.length + "");
//                for (int i = 0; i < mSplStart.length; i++) {
//                    MYLOG.LOGI(" the start " + mSplStart[i]);
//                }
                pyStr = mIPinyinDecoderService.imGetPyStr(false);//这个是全部
                mSurfaceDecodedLen = mIPinyinDecoderService.imGetPyStrLen(true);//已经decode了的长度

                mFullSent = mIPinyinDecoderService.imGetChoice(0);
                mFixedLen = mIPinyinDecoderService.imGetFixedLen();

                //这里开始是从engin中读取数据
                mSurface.replace(0, mSurface.length(), pyStr);//更行之前输入的因为由于解析发生了变化
                mCursorPos = mCursorPos > length()? length() : mCursorPos;
                //如果已经有了选定的词语的话更新下
                mComposingStr = mFullSent.substring(0, mFixedLen)
                        + mSurface.substring(mSplStart[mFixedLen + 1]);

                mActiveComsLen = mComposingStr.length();
                if (mSurfaceDecodedLen > 0) { //已经有被解析的
                    mActiveComsLen = mActiveComsLen - (length() - mSurfaceDecodedLen);
                }

                //更新要展示的内容
                if (0 == mSurfaceDecodedLen) {
                    mComposingStrDisplay = mComposingStr;
                    mActiveCmpsDisplayLen = mActiveComsLen;
                } else {
                    mComposingStrDisplay = mFullSent.substring(0, mFixedLen);
                    //MYLOG.LOGI("fix string : " + mComposingStr);
                    for (int pos = mFixedLen + 1; pos < mSplStart.length - 1; pos++) {
                        mComposingStrDisplay +=
                                mSurface.substring(mSplStart[pos], mSplStart[pos + 1]);
                        //MYLOG.LOGI("after add the next : " + mComposingStr);
                        if (mSplStart[pos + 1] < mSurfaceDecodedLen) {
                            mComposingStrDisplay += " ";
                        }
                    }
                    mActiveCmpsDisplayLen = mComposingStrDisplay.length();
                    if (mSurfaceDecodedLen < mSurface.length()) {
                        mComposingStrDisplay += mSurface.substring(mSurfaceDecodedLen);//剩下的就是无法解析的
                    }
                    if (mSplStart.length == mFixedLen + 2) { //选完后
                        mFinishSelection = true;
                    } else {
                        mFinishSelection = false;
                    }
                }
            } catch (RemoteException re) {
                MYLOG.LOGW(re.toString());
            } catch (Exception e) {
                mTatalChoicesNum = 0;
                mComposingStr = "";
            }

            if (!mFinishSelection) {
                //加载一波
                loadMore(true);
//                MYLOG.LOGI("prepare page 0");
            }
        }

        /**
         * 选择了预测时候的词语 上面的 那个是compsoing时候的查词库
         * @param choiceId
         */
        private void choosePredictChoice(int choiceId) {
            if (ImeState.STATE_PREDICT != mImeState || choiceId < 0
                    || choiceId >= mTatalChoicesNum) {
                return;
            }

            String temp = mCandidatesList.get(choiceId);
            resetCandidates();
            mCandidatesList.add(temp);
            mTatalChoicesNum = 1;
            mSurface.replace(0, mSurface.length(),"");
            mCursorPos = 0;
            mFullSent = temp;
            mFixedLen = temp.length();
            mComposingStr = mFullSent;
            mActiveComsLen = mFixedLen;
            mFinishSelection = true;
            candidatesChange();
        }

        public String getCandidate(int canId) {
            if (canId < 0 || canId > mCandidatesList.size()) return null;
            return mCandidatesList.get(canId);
        }

        /**
         * 判断前一个是不是分词符号
         * @return
         */
        public boolean charBeforeCursorIsSeparator() {
            int len = mSurface.length();
            if (mCursorPos > len) return false;
            if (mCursorPos > 0 && mSurface.charAt(mCursorPos - 1) == '\'') {
                return true;
            }
            return false;
        }

        public int getCursorPosInCmps() {
            int cursorpos = mCursorPos;
            for (int hzPos = 0; hzPos < mFixedLen; hzPos++) {
                if (mCursorPos >= mSplStart[hzPos]) {
                    cursorpos = cursorpos - (mSplStart[hzPos + 2] - mSplStart[hzPos + 1]);
                    cursorpos ++;
                }
            }
            return cursorpos;
        }

        public int getCursorPosInCmpsDisplay() {
            int cursorPos = getCursorPosInCmps();
            //+2 一是因为 第一个用作其他用途 , 第二是因为 第一个没有空格在他之前
            for (int pos = mFixedLen + 2; pos < mSplStart.length - 1; pos++) {
                if (mCursorPos <= mSplStart[pos]) {
                    break;
                } else {
                    cursorPos++;
                }
            }
            return cursorPos;
        }
        public void moveCursorToEdge(boolean left) {
            if (left) {
                mCursorPos = 0;
            } else {
                mCursorPos = mSurface.length();
            }
        }

        /**
         * @param offset 如果为0 就作为调节 cursor到compsing合适位置的方法
         */
        public void moveCursor(int offset) {
            if (offset > 1 || offset < -1) return;
            if (0 != offset) {
                int hzPos;
                for (hzPos = 0; hzPos <= mFixedLen; hzPos++) {
                    if (mCursorPos == mSplStart[hzPos + 1]) {
                        if (offset < 0) { //向左
                            if (hzPos > 0) {
                                offset = mSplStart[hzPos] - mSplStart[hzPos + 1];
                            }
                        } else {
                            if (hzPos < mFixedLen) {
                                offset = mSplStart[hzPos + 2] - mSplStart[1];//往后移一个单位
                            }
                        }
                        break;
                    }
                }
            }
            mCursorPos += offset;
            if (mCursorPos < 0) {
                mCursorPos = 0;
            } else if (mCursorPos > mSurface.length()) {
                mCursorPos = mSurface.length();
            }
        }

        public int getFixedLen() {
            return mFixedLen;
        }

    }

    /**
     * 以下将是浮动窗口相关的的各种东西
     */

    public class MyFloatModeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SWITCH_FLOAT_MODE:
                    //燃鹅还的做些别的辅助准备
                    removeMessages(MSG_SWITCH_FLOAT_MODE);
                    switch2FlatMode();
                    break;
                case MSG_SHOW_FLOAT_MODE_OVERLAY_WINDOW:
                    removeMessages(MSG_SHOW_FLOAT_MODE_OVERLAY_WINDOW);
                    showFloatModeOverlayWindow(true);
                    break;
                case MSG_SHOW_FLOAT_RESIZE_WINDOW:
                    removeMessages(MSG_SHOW_FLOAT_RESIZE_WINDOW);
                    resizeKeyboardSize();
                    break;
                case MSG_SHOW_FLOAT_MODE_POP_WINDOW:
                    removeMessages(MSG_SHOW_FLOAT_MODE_POP_WINDOW);
                    showTheFloatPopWindow();
                    break;
                case MSG_START_INPUT_READ_CONFIG:
                    mEnvironment.readFloatModeInfoFromSharePre(CPLIME.this);
                    if (mEnvironment.isFloatMode()) {
                        loadFoatWindowConfig();
                    }
                    break;
            }
        }
    }

    private void switch2FlatMode() {
        clearKeyboardStatus();
        boolean isFloat = mEnvironment.isFloatMode();
        isFloat = !isFloat;
        mEnvironment.setFloatMode(isFloat);
        mEnvironment.saveIsFloat2SharePre(this);

        loadFoatWindowConfig();
    }

    /**
     * 清空之前的所有状态 预留
     */
    private void clearKeyboardStatus() {
        if(null != mFloatKeyboardPopup && mFloatKeyboardPopup.isShowing()
                && mFloatKeyboardPopup.getMyRootLayout() != null
                && mFloatKeyboardPopup.getMyRootLayout().getWindowToken() != null) {
            mFloatKeyboardPopup.dismiss();
        }

        if (null != mResizePopup && mResizePopup.isShowing()) {
            mResizePopup.dismiss();
        }

        dismissFloatModeOverlayWindow();
    }

    /**
     * 重新读取各个窗口的状态 并调整至合适状态
     */
    private void loadFoatWindowConfig() {
        if (!isCanFloatMode()) return;
        mFunctionCandidateContainer.setResizeable(mEnvironment.isFloatMode());
        //点击本按钮后开始模式
        mEnvironment.resetWindowSize(this, getResources().getConfiguration());//重新设置下大小
        SoftKeyboardPool.getInstance()
                .setSoftKeyboardCoreSize(mEnvironment.getSkbWidth(), mEnvironment.getSkbHeight());//
        mNormalCandidatesContainer.requestLayout();
        mFuctionAndNormalCndLayout.requestLayout();
        if (mEnvironment.isFloatMode()) {
            updateTheFloatModeLocation();
        } else { //如果不能浮动窗口模式该影藏的隐藏
            WindowManager.LayoutParams lp
                    = ((InputMethodService) this).getWindow().getWindow().getAttributes();
            lp.width = ViewGroup.LayoutParams.FILL_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.x = 0;
            lp.y = 0;
            lp.flags = lp.flags & ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            this.getWindow().getWindow().setAttributes(lp);
            if (mFloatKeyboardView != null) {
                mFloatKeyboardView.removeAllViews();
            }
            clearKeyboardStatus();

            setInputView(mSkbContainer);
            setCandidatesView(mFuctionAndNormalCndLayout);
            resetToIdleState(false);
            setShowFunctionCandidate(true);
            setAlpha(mSkbContainer, 1f);
            setAlpha(mFuctionAndNormalCndLayout, 1f);
        }
    }

    private void updateTheFloatModeLocation() {
        createFloatRootView();
        //// TODO: 16年3月17日 根据背景进行一定的背景处理 不过此刻先不弄
//        这个是第一种解决方案
//        WindowManager.LayoutParams lp
//                = ((InputMethodService) this).getWindow().getWindow().getAttributes();
//        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
//        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
//        lp.gravity = Gravity.NO_GRAVITY;
//        lp.x = 0;
//        lp.y = 0;
//        lp.flags = lp.flags | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE & ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
//        lp.softInputMode = lp.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
//        this.getWindow().getWindow().setAttributes(lp);

        //第三套解决方案和windowMangaer无关

        if (null != mFloatCandidateView.getParent()) {
            ((ViewGroup)mFloatCandidateView.getParent()).removeView(mFloatCandidateView);
        }
        if (null != mFloatRootView.getParent()) {
            ((ViewGroup)mFloatRootView.getParent()).removeView(mFloatRootView);
        }
        if (null != mBlankLayout.getParent()) {
            ((ViewGroup)mBlankLayout.getParent()).removeView(mBlankLayout);
        }

        //重新设置一下窗口
        setCandidatesView(mFloatCandidateView);
//        setInputView(mFloatRootView);
        setInputView(mBlankLayout);



        mFloatKeyboardView.removeAllViews();
        mFloatKeyboardView.addView(mFuctionAndNormalCndLayout);
        mFloatKeyboardView.addView(mSkbContainer);

//        下面这个方案是搭配全屏的inputlayout来做的
//        FrameLayout.LayoutParams rootViewLp = (FrameLayout.LayoutParams) mFloatRootView.getLayoutParams();
//        rootViewLp.leftMargin = leftPadding;
//        rootViewLp.topMargin = topPadding;
//        mFloatRootView.setLayoutParams(rootViewLp);

        //发送handle更新出来
//        mFloatModeHandle.sendEmptyMessageDelayed(MSG_SHOW_FLOAT_MODE_OVERLAY_WINDOW, 20);

//        第三方案
        mFloatKeyboardPopup.removeMyView();
        mFloatKeyboardPopup.addMyView(mFloatRootView);
        mFloatModeHandle.sendEmptyMessageDelayed(MSG_SHOW_FLOAT_MODE_POP_WINDOW, 1);

    }


    private void showTheFloatPopWindow() {
        int inputWidth = mEnvironment.getSkbWidth();
        int inputHeight = mEnvironment.getHeightForCandidates() + mEnvironment.getSkbHeight();
        mFloatKeyboardPopup.setWidth(inputWidth);
        mFloatKeyboardPopup.setHeight(inputHeight);
        mFloatKeyboardPopup.setBackgroundDrawable(null);
        mFloatKeyboardPopup.setClippingEnabled(false);
        int leftMargin = mEnvironment.getFloatingModeLocationX(getApplicationContext());
        int topMargin = mEnvironment.getFloatingModeLocationY(getApplicationContext());
        mFloatKeyboardPopup.setWindowLayoutType(
                WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
//        mFloatKeyboardPopup.setSoftInputMode(WindowManager.LayoutParams.Input);
        if (mFloatKeyboardPopup.isShowing()) {
            mFloatKeyboardPopup.update(leftMargin, topMargin, mFloatKeyboardPopup.getWidth(),
                    mFloatKeyboardPopup.getHeight());
        } else {
            mFloatKeyboardPopup.showAtLocation(mFloatCandidateView,
                    Gravity.NO_GRAVITY, leftMargin, topMargin);
        }
        mFloatKeyboardPopup.getMyRootLayout().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dispatchTouchEventToKeyboard(event);
            }
        });
    }

    /**
     * 单独创建放各个合适控件的layout
     */
    private void createFloatRootView() {
        if (null == mFloatRootView) {
            LayoutInflater mInflater = getLayoutInflater();
            mFloatRootView = (RelativeLayout) mInflater.inflate(R.layout.float_root_layout, null);
            mFloatKeyboardView = (LinearLayout) mFloatRootView.findViewById(R.id.float_keyboard_view);
            mFloatCandidateView = (RelativeLayout) mInflater.inflate(R.layout.float_candidate_layout, null);
            mBlankLayout = (LinearLayout) mInflater.inflate(R.layout.float_blank_layout, null);
        }
        if (null == mFloatKeyboardPopup) {
            mFloatKeyboardPopup = new FloatContainerPopup(this);
        }
    }

    /**
     * 如果是物理键盘的话就不行
     * todo 将来判断
     * return
     */
    private boolean isCanFloatMode() {
        return true;
    }

    /**
     * 调整合适的padding并且保存方便下次直接按照设定读取
     */
    private void checkFloatModeLocation() {
        if (null != mFuctionAndNormalCndLayout && null != mSkbContainer) {
            int leftPadding = mEnvironment.getFloatingModeLocationX(getApplicationContext());
            int topPadding = mEnvironment.getFloatingModeLocationY(getApplicationContext());
            Rect frame = new Rect();
            mFuctionAndNormalCndLayout.getWindowVisibleDisplayFrame(frame);
            if (topPadding + mFuctionAndNormalCndLayout.getHeight() + mSkbContainer.getHeight() > frame.height()) {
                topPadding = frame.height() - mFuctionAndNormalCndLayout.getHeight() - mSkbContainer.getHeight();
                mEnvironment.setFloatingModeLocation(leftPadding, topPadding, getApplicationContext());
            }
        }
    }

    /***
     * 弹出resize的popup
     */
    public void resizeKeyboardSize() {
        if (null == mResizePopup) {
            mResizePopup = new ResizePop(this);
            mResizePopup.setListener(mResizeListener);
            mResizePopup.setWindowLayoutType(
                    WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
            mResizePopup.setBackgroundDrawable(null);
        }
        int inputWidth = mEnvironment.getSkbWidth();
        int inputHeight = mFuctionAndNormalCndLayout.getHeight()
                + mSkbContainer.getSoftkeyboardView().getHeight();
        int leftPadding = mEnvironment.getFloatingModeLocationX(getApplicationContext());
        int topPadding = mEnvironment.getFloatingModeLocationY(getApplicationContext());
        mResizePopup.setRootWidth(inputWidth);
        mResizePopup.setRootHeight(inputHeight);
        if(mResizePopup.isShowing()) {
            mResizePopup.update(leftPadding, topPadding, mResizePopup.getWidth(), mResizePopup.getHeight());
        } else {
            mResizePopup.showAtLocation(mBlankLayout,
                    Gravity.NO_GRAVITY, leftPadding, topPadding);
        }
    }

    /**
     * 稍微添加个淡出淡入效果
     * @param v
     * @param alpha
     */
    private void setAlpha(View v, float alpha){
        if(v==null)return;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            AlphaAnimation mainInAnimation = new AlphaAnimation(alpha, alpha);
            mainInAnimation.setDuration(0);
            mainInAnimation.setFillAfter(true);
            v.startAnimation(mainInAnimation);
        } else {
            v.setAlpha(alpha);
        }
    }

    /**
     * 这个弹出的popWindow相当于一个蒙版用来准备的一些操作的方便使用
     * @param force
     * @Deprecated
     */
    @Deprecated
    private void showFloatModeOverlayWindow(boolean force) {
        if (!isInputViewShown() ||
                !(mFuctionAndNormalCndLayout != null && mFuctionAndNormalCndLayout.isShown()
                    && mFuctionAndNormalCndLayout.getWindowToken() != null)) { //不符合条件
            if (force) {
                mFloatingWindowTimer.postDelayed(mFloatingWindowTimer, 20);
                return;
            }
        }
        if (null == mFuctionAndNormalCndLayout || null == mSkbContainer) {
            return;
        }
        if (null == mFloatModeOverlayWindow) {
            createFloatModeOverlayWindow();
        }
        MYLOG.LOGI("now to show");

        //刷新以下各种属性的高度
        int inputHeight = mFuctionAndNormalCndLayout.getHeight()
                + mSkbContainer.getSoftkeyboardView().getHeight();
        int inputWidth = mEnvironment.getSkbWidth();
        mFloatModeOverlayWindow.setWidth(inputWidth);
        mFloatModeOverlayWindow.setHeight(inputHeight);
        int leftPadding = mEnvironment.getFloatingModeLocationX(getApplicationContext());
        int topPadding = mEnvironment.getFloatingModeLocationY(getApplicationContext());
        if (mFloatModeOverlayWindow.isShowing()) {

            mFloatModeOverlayWindow.update(leftPadding, topPadding, inputWidth, inputHeight);
        } else {
            MYLOG.LOGI("the window token same? " + (mNormalCandidatesContainer.getWindowToken() == mFloatCandidateView.getWindowToken()));
            mFloatModeOverlayWindow.showAtLocation(mFloatCandidateView,
                    Gravity.NO_GRAVITY, leftPadding, topPadding);
        }
        setAlpha(mSkbContainer, 1f);
        setAlpha(mFuctionAndNormalCndLayout, 1f);
    }

    /**
     * 创建悬浮的蒙板window
     */
    @Deprecated
    private void createFloatModeOverlayWindow() {
        mFloatModeOverlayView = new View(this);
        mFloatModeOverlayView.setBackgroundDrawable(
              new ColorDrawable(getResources().getColor(R.color.half_transparent)));
//        mFloatModeOverlayView.setBackgroundDrawable(null);
        mFloatModeOverlayWindow = new PopupWindow(mFloatModeOverlayView,
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        mFloatModeOverlayWindow.setBackgroundDrawable(null);
        mFloatModeOverlayWindow.setClippingEnabled(false);
        mFloatModeOverlayWindow.setOutsideTouchable(false);
        mFloatModeOverlayWindow.setTouchable(true);
        mFloatModeOverlayWindow.setFocusable(false);
        //分发点击事件如果对应的东西被点了
        mFloatModeOverlayView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return dispatchTouchEventToKeyboard(event);
            }
        });
    }

    /**
     * 这个方法极其重要如果用这套方案来做的话
     * @param event
     * @return
     */
    @Deprecated
    private boolean dispatchTouchEventToKeyboard(MotionEvent event) {
        if (null != mFloatKeyboardView && mFloatKeyboardView.isShown()) {
//            if (event.getAction() == MotionEvent.ACTION_CANCEL) {
//                MYLOG.LOGI("origin action cancel");
//            } else {
//                MYLOG.LOGI("origin action " + event.getAction());
//            }
            if (mEnvironment.isFloatMode() && mFunctionCandidateContainer.isLongPress() &&
                    (MotionEvent.ACTION_MOVE == event.getAction()
                            || MotionEvent.ACTION_UP == event.getAction())) { //这里写到耦合有点高
                return mFunctionCandidateContainer.onTouchEvent(event);
            }
            return mFloatKeyboardView.dispatchTouchEvent(event);
        }
        return true;
    }

    private void dismissFloatModeOverlayWindow() {
        if (null != mFloatModeOverlayWindow && mFloatModeOverlayWindow.isShowing()
                && null != mNormalCandidatesContainer.getWindowToken()) {
            mFloatModeOverlayWindow.dismiss();
        }
    }


    /**
     * 这个是浮动窗口拖动
     */
    public interface FloatWindowDragListener {
        void dragFloatWindow(float dx, float dy);
    }

    public class FloatWindowDragListenerImpl implements FloatWindowDragListener {
        @Override
        public void dragFloatWindow(float dx, float dy) {
            updateTheFloatModeLocation();
        }
    }

    /**
     * 这个是浮动状态下调整大小
     */
    public interface RestSizeFinishListener {
        void resetSizeFinish();
    }

    public class resetSizeFinishListenerImpl implements RestSizeFinishListener {
        @Override
        public void resetSizeFinish() {
            loadFoatWindowConfig();
        }
    }
}

